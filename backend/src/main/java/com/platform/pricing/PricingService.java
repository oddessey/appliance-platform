package com.platform.pricing;

import com.platform.appliance.ApplianceTypeRepository;
import com.platform.pricing.PricingRepository.PricingConditions;
import com.platform.pricing.PricingRepository.PricingItem;
import com.platform.pricing.PricingRepository.QuoteAcceptanceState;
import com.platform.pricing.PricingRepository.QuoteLineDraft;
import com.platform.pricing.PricingRepository.PricingRequestState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PricingService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int QUOTE_VALIDITY_DAYS = 7;
    private static final int MAX_PAGE_SIZE = 100;

    private final PricingRepository repository;
    private final ApplianceTypeRepository applianceTypeRepository;

    public PricingService(PricingRepository repository, ApplianceTypeRepository applianceTypeRepository) {
        this.repository = repository;
        this.applianceTypeRepository = applianceTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<PriceRuleResponse> listPriceRules() {
        return repository.findPriceRules().stream().map(PriceRule::toResponse).toList();
    }

    @Transactional
    public PriceRuleResponse createPriceRule(PriceRuleRequest request, UUID actorId) {
        validateRule(request);
        PriceRule created = repository.createPriceRule(UUID.randomUUID(), request, actorId);
        repository.recordPriceRuleAudit(actorId, "PRICE_RULE_CREATED", created, null);
        return created.toResponse();
    }

    @Transactional
    public PriceRuleResponse updatePriceRule(UUID ruleId, PriceRuleRequest request, UUID actorId) {
        validateRule(request);
        PriceRule before = repository.findPriceRule(ruleId).orElseThrow(PriceRuleNotFoundException::new);
        PriceRule updated = repository.updatePriceRule(ruleId, request);
        repository.recordPriceRuleAudit(actorId, "PRICE_RULE_UPDATED", updated, before);
        return updated.toResponse();
    }

    @Transactional(readOnly = true)
    public PricingRequestPageResponse listPricingRequests(int page, int size) {
        validatePage(page, size);
        return new PricingRequestPageResponse(page, size, repository.countPricingRequests(),
                repository.findPricingRequests(size, page * size));
    }

    @Transactional
    public QuoteResponse issueQuote(UUID requestId, UUID actorId) {
        PricingRequestState requestState = repository.lockPricingRequest(requestId)
                .orElseThrow(PricingRequestNotFoundException::new);
        if (!List.of("SUBMITTED", "QUOTED").contains(requestState.status())
                || requestState.quoteAccepted() || requestState.hasReservation()) {
            throw new QuoteNotAcceptableException();
        }

        List<PricingItem> items = repository.findPricingItems(requestId);
        if (items.isEmpty()) {
            throw new PriceRuleNotConfiguredException();
        }
        List<PriceRule> rules = repository.findEffectivePriceRules(OffsetDateTime.now());
        List<QuoteLineDraft> lines = calculateLines(items, rules);
        BigDecimal subtotal = sumAmounts(lines, false);
        BigDecimal discount = sumAmounts(lines, true).min(subtotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal tax = taxable.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = taxable.add(tax).setScale(2, RoundingMode.HALF_UP);

        OffsetDateTime issuedAt = OffsetDateTime.now();
        OffsetDateTime expiresAt = issuedAt.plusDays(QUOTE_VALIDITY_DAYS);
        int version = repository.nextQuoteVersion(requestId);
        UUID quoteId = UUID.randomUUID();
        String quoteNo = "QT-" + issuedAt.toLocalDate().toString().replace("-", "")
                + "-" + quoteId.toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        PricingConditions conditions = conditionsFrom(items);

        repository.expireIssuedQuotes(requestId);
        repository.insertQuote(quoteId, quoteNo, requestId, version, subtotal, discount, tax, total,
                issuedAt, expiresAt, conditions, TAX_RATE.multiply(ONE_HUNDRED));
        lines.forEach(line -> repository.insertQuoteLine(quoteId, line));
        repository.snapshotAppliedRules(quoteId);
        repository.updateRequestAsQuoted(requestId);
        repository.recordQuoteAudit(actorId, quoteId, "QUOTE_ISSUED", quoteNo, version, total);

        return repository.findQuote(requestId, quoteId).orElseThrow(QuoteNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public QuotePageResponse listCustomerQuotes(UUID requestId, UUID customerId, int page, int size) {
        validatePage(page, size);
        if (!repository.ownsRequest(requestId, customerId)) {
            throw new PricingRequestNotFoundException();
        }
        return new QuotePageResponse(page, size, repository.countQuotes(requestId),
                repository.findQuotes(requestId, size, page * size));
    }

    @Transactional
    public void acceptQuote(UUID quoteId, UUID customerId) {
        QuoteAcceptanceState quote = repository.lockQuoteForAcceptance(quoteId, customerId)
                .orElseThrow(QuoteNotFoundException::new);
        if (!"ISSUED".equals(quote.status()) || !"QUOTED".equals(quote.requestStatus())
                || !quote.latest() || quote.expiresAt() == null || !quote.expiresAt().isAfter(OffsetDateTime.now())) {
            throw new QuoteNotAcceptableException();
        }
        repository.acceptQuote(quoteId, quote.requestId());
        repository.recordQuoteAudit(customerId, quoteId, "QUOTE_ACCEPTED", null, null, null);
    }

    private List<QuoteLineDraft> calculateLines(List<PricingItem> items, List<PriceRule> rules) {
        List<QuoteLineDraft> lines = new ArrayList<>();
        for (PricingItem item : items) {
            PriceRule baseRule = selectBaseRule(item, rules);
            lines.add(toLine(baseRule, item.applianceTypeCode(), BigDecimal.valueOf(item.quantity())));

            for (PriceRule rule : rules) {
                if (!matchesAppliance(rule, item.applianceTypeCode())
                        || rule.conditionCode() == PriceConditionCode.BASE
                        || rule.conditionCode() == PriceConditionCode.DISCOUNT) {
                    continue;
                }
                BigDecimal quantity = conditionQuantity(rule.conditionCode(), item)
                        .multiply(BigDecimal.valueOf(item.quantity()));
                if (quantity.signum() > 0) {
                    lines.add(toLine(rule, item.applianceTypeCode(), quantity));
                }
            }
        }

        BigDecimal preDiscountTotal = sumAmounts(lines, false);
        for (PriceRule rule : rules) {
            if (rule.conditionCode() != PriceConditionCode.DISCOUNT) {
                continue;
            }
            BigDecimal eligibleSubtotal = lines.stream()
                    .filter(line -> line.conditionCode() != PriceConditionCode.DISCOUNT)
                    .filter(line -> rule.applianceTypeCode() == null
                            || rule.applianceTypeCode().equals(line.applianceTypeCode()))
                    .map(QuoteLineDraft::lineAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .min(preDiscountTotal);
            BigDecimal discountAmount = eligibleSubtotal.multiply(rule.amount())
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
                    .min(preDiscountTotal.subtract(sumAmounts(lines, true)).max(BigDecimal.ZERO));
            if (discountAmount.signum() > 0) {
                lines.add(new QuoteLineDraft(rule.ruleCode(), rule.name() + " 할인",
                        BigDecimal.ONE, discountAmount, discountAmount,
                        rule.id(), rule.version(), rule.conditionCode(), rule.calculationType(),
                        rule.amount(), rule.applianceTypeCode()));
            }
        }
        if (lines.stream().noneMatch(line -> line.conditionCode() == PriceConditionCode.BASE)) {
            throw new PriceRuleNotConfiguredException();
        }
        return lines;
    }

    private PriceRule selectBaseRule(PricingItem item, List<PriceRule> rules) {
        List<PriceRule> matches = rules.stream()
                .filter(rule -> rule.conditionCode() == PriceConditionCode.BASE)
                .filter(rule -> matchesAppliance(rule, item.applianceTypeCode()))
                .sorted(Comparator.comparingInt(PriceRule::priority).reversed()
                        .thenComparing(rule -> rule.applianceTypeCode() == null ? 1 : 0))
                .toList();
        if (matches.isEmpty()) {
            throw new PriceRuleNotConfiguredException();
        }
        PriceRule selected = matches.getFirst();
        if (matches.size() > 1
                && matches.get(1).priority() == selected.priority()
                && isSpecific(matches.get(1), item.applianceTypeCode())
                    == isSpecific(selected, item.applianceTypeCode())) {
            throw new InvalidPriceRuleException();
        }
        return selected;
    }

    private QuoteLineDraft toLine(PriceRule rule, String applianceTypeCode, BigDecimal quantity) {
        BigDecimal lineAmount = rule.amount().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        return new QuoteLineDraft(rule.ruleCode(),
                rule.name(),
                quantity.setScale(2, RoundingMode.HALF_UP),
                rule.amount().setScale(2, RoundingMode.HALF_UP),
                lineAmount, rule.id(), rule.version(), rule.conditionCode(), rule.calculationType(),
                rule.amount(), applianceTypeCode);
    }

    private BigDecimal conditionQuantity(PriceConditionCode conditionCode, PricingItem item) {
        return switch (conditionCode) {
            case CARRYING_DISTANCE -> item.carryingDistanceMeters();
            case FLOOR -> item.floorCount();
            case ADDITIONAL_PIPING -> item.additionalPipingMeters();
            case BASE, DISCOUNT -> BigDecimal.ZERO;
        };
    }

    private BigDecimal sumAmounts(List<QuoteLineDraft> lines, boolean discountsOnly) {
        return lines.stream()
                .filter(line -> (line.conditionCode() == PriceConditionCode.DISCOUNT) == discountsOnly)
                .map(QuoteLineDraft::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private PricingConditions conditionsFrom(List<PricingItem> items) {
        PricingItem first = items.getFirst();
        return new PricingConditions(first.carryingDistanceMeters().intValue(),
                first.floorCount().intValue(), first.additionalPipingMeters());
    }

    private boolean matchesAppliance(PriceRule rule, String applianceTypeCode) {
        return rule.applianceTypeCode() == null || rule.applianceTypeCode().equals(applianceTypeCode);
    }

    private boolean isSpecific(PriceRule rule, String applianceTypeCode) {
        return applianceTypeCode.equals(rule.applianceTypeCode());
    }

    private void validateRule(PriceRuleRequest request) {
        if (request.priority() < -10000 || request.priority() > 10000
                || (request.validFrom() != null && request.validTo() != null
                    && !request.validTo().isAfter(request.validFrom()))) {
            throw new InvalidPriceRuleException();
        }
        boolean validCalculation = switch (request.conditionCode()) {
            case BASE -> request.calculationType() == PriceCalculationType.FIXED;
            case CARRYING_DISTANCE, FLOOR, ADDITIONAL_PIPING ->
                    request.calculationType() == PriceCalculationType.PER_UNIT;
            case DISCOUNT -> request.calculationType() == PriceCalculationType.PERCENTAGE
                    && request.amount().compareTo(ONE_HUNDRED) <= 0;
        };
        if (!validCalculation) {
            throw new InvalidPriceRuleException();
        }
        if (request.applianceTypeCode() != null
                && applianceTypeRepository.findActiveByCode(request.applianceTypeCode()).isEmpty()) {
            throw new InvalidPriceRuleException();
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE || (long) page * size > Integer.MAX_VALUE) {
            throw new InvalidPricingPageException();
        }
    }
}
