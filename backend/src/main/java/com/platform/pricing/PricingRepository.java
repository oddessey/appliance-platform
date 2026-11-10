package com.platform.pricing;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PricingRepository {

    private final PricingMapper mapper;

    public PricingRepository(PricingMapper mapper) {
        this.mapper = mapper;
    }

    public List<PriceRule> findPriceRules() {
        return mapper.findPriceRules();
    }

    public Optional<PriceRule> findPriceRule(UUID id) {
        return Optional.ofNullable(mapper.findPriceRule(id));
    }

    public PriceRule createPriceRule(UUID id, PriceRuleRequest request, UUID actorId) {
        try {
            return mapper.createPriceRule(id, request, actorId);
        } catch (DuplicateKeyException exception) {
            throw new PriceRuleConflictException();
        }
    }

    public PriceRule updatePriceRule(UUID id, PriceRuleRequest request) {
        try {
            PriceRule updated = mapper.updatePriceRule(id, request);
            if (updated == null) {
                throw new PriceRuleNotFoundException();
            }
            return updated;
        } catch (DuplicateKeyException exception) {
            throw new PriceRuleConflictException();
        }
    }

    public void recordPriceRuleAudit(UUID actorId, String action, PriceRule after, PriceRule before) {
        String beforeSummary = before == null ? null : """
                {"ruleCode":"%s","version":%d,"active":%s,"amount":%s}
                """.formatted(before.ruleCode(), before.version(), before.active(),
                before.amount().toPlainString()).strip();
        mapper.recordPriceRuleAudit(UUID.randomUUID(), actorId, action, after.id(), beforeSummary, after);
    }

    public void recordQuoteAudit(
            UUID actorId,
            UUID quoteId,
            String action,
            String quoteNo,
            Integer version,
            BigDecimal total
    ) {
        mapper.recordQuoteAudit(UUID.randomUUID(), actorId, action, quoteId, quoteNo, version, total);
    }

    public List<PriceRule> findEffectivePriceRules(OffsetDateTime now) {
        return mapper.findEffectivePriceRules(now);
    }

    public Optional<PricingRequestState> lockPricingRequest(UUID requestId) {
        return Optional.ofNullable(mapper.lockPricingRequest(requestId));
    }

    public List<PricingItem> findPricingItems(UUID requestId) {
        return mapper.findPricingItems(requestId);
    }

    public long countPricingRequests() {
        return mapper.countPricingRequests();
    }

    public List<PricingRequestSummary> findPricingRequests(int limit, int offset) {
        return mapper.findPricingRequests(limit, offset);
    }

    public int nextQuoteVersion(UUID requestId) {
        Integer version = mapper.nextQuoteVersion(requestId);
        return version == null ? 1 : version;
    }

    public void expireIssuedQuotes(UUID requestId) {
        mapper.expireIssuedQuotes(requestId);
    }

    public void insertQuote(
            UUID id,
            String quoteNo,
            UUID requestId,
            int version,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal tax,
            BigDecimal total,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt,
            PricingConditions conditions,
            BigDecimal taxRate
    ) {
        mapper.insertQuote(id, quoteNo, requestId, version, subtotal, discount, tax, total,
                conditions, taxRate, issuedAt, expiresAt);
    }

    public void insertQuoteLine(UUID quoteId, QuoteLineDraft line) {
        mapper.insertQuoteLine(UUID.randomUUID(), quoteId, line);
    }

    public void snapshotAppliedRules(UUID quoteId) {
        mapper.snapshotAppliedRules(quoteId);
    }

    public void updateRequestAsQuoted(UUID requestId) {
        mapper.updateRequestAsQuoted(requestId);
    }

    public boolean ownsRequest(UUID requestId, UUID customerId) {
        return Boolean.TRUE.equals(mapper.ownsRequest(requestId, customerId));
    }

    public List<QuoteResponse> findQuotes(UUID requestId, int limit, int offset) {
        List<PricingMapper.QuoteHeader> headers = mapper.findQuotes(requestId, limit, offset);
        if (headers.isEmpty()) {
            return List.of();
        }
        List<UUID> quoteIds = headers.stream().map(PricingMapper.QuoteHeader::id).toList();
        Map<UUID, List<QuoteLineResponse>> linesByQuote = new HashMap<>();
        for (PricingMapper.QuoteLineWithQuoteId line : mapper.findQuoteLines(quoteIds)) {
            linesByQuote.computeIfAbsent(line.quoteId(), ignored -> new ArrayList<>()).add(
                    new QuoteLineResponse(line.lineCode(), line.description(), line.quantity(),
                            line.unitPrice(), line.lineAmount()));
        }
        return headers.stream().map(header -> toQuoteResponse(
                header, linesByQuote.getOrDefault(header.id(), List.of()))).toList();
    }

    public long countQuotes(UUID requestId) {
        return mapper.countQuotes(requestId);
    }

    public Optional<QuoteResponse> findQuote(UUID requestId, UUID quoteId) {
        return Optional.ofNullable(mapper.findQuote(requestId, quoteId))
                .map(header -> toQuoteResponse(header, mapper.findQuoteLinesForQuote(quoteId)));
    }

    public Optional<QuoteAcceptanceState> lockQuoteForAcceptance(UUID quoteId, UUID customerId) {
        return Optional.ofNullable(mapper.lockQuoteForAcceptance(quoteId, customerId));
    }

    public void acceptQuote(UUID quoteId, UUID requestId) {
        mapper.expireOtherIssuedQuotes(requestId, quoteId);
        mapper.acceptQuote(quoteId);
    }

    private QuoteResponse toQuoteResponse(
            PricingMapper.QuoteHeader header,
            List<QuoteLineResponse> lines
    ) {
        return new QuoteResponse(
                header.id(), header.quoteNo(), header.versionNo(), header.status(), header.currency(),
                header.subtotalAmount(), header.discountAmount(), header.taxAmount(), header.totalAmount(),
                header.issuedAt(), header.expiresAt(), lines);
    }

    public record PricingRequestState(String status, boolean quoteAccepted, boolean hasReservation) {
    }

    public record PricingItem(
            String applianceTypeCode,
            int quantity,
            BigDecimal carryingDistanceMeters,
            BigDecimal floorCount,
            BigDecimal additionalPipingMeters
    ) {
    }

    public record PricingConditions(
            int carryingDistanceMeters,
            int floorCount,
            BigDecimal additionalPipingMeters
    ) {
    }

    public record QuoteLineDraft(
            String lineCode,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineAmount,
            UUID ruleId,
            int ruleVersion,
            PriceConditionCode conditionCode,
            PriceCalculationType calculationType,
            BigDecimal configuredAmount,
            String applianceTypeCode
    ) {
    }

    public record QuoteAcceptanceState(
            UUID requestId,
            String status,
            OffsetDateTime expiresAt,
            String requestStatus,
            boolean latest
    ) {
    }
}
