package com.platform.pricing;

import com.platform.appliance.ApplianceTypeRepository;
import com.platform.pricing.PricingRepository.PricingItem;
import com.platform.pricing.PricingRepository.PricingRequestState;
import com.platform.pricing.PricingRepository.QuoteLineDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PricingRepository repository;
    @Mock
    private ApplianceTypeRepository applianceTypeRepository;
    @InjectMocks
    private PricingService pricingService;

    @Test
    void issuesQuoteFromBaseSurchargesAndDiscountAndPreservesRuleSnapshots() {
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        List<PriceRule> rules = List.of(
                rule("AC_BASE", PriceConditionCode.BASE, PriceCalculationType.FIXED, "100000", "AIR_CONDITIONER", 10),
                rule("CARRY", PriceConditionCode.CARRYING_DISTANCE, PriceCalculationType.PER_UNIT, "100", null, 10),
                rule("FLOOR", PriceConditionCode.FLOOR, PriceCalculationType.PER_UNIT, "5000", null, 10),
                rule("PIPE", PriceConditionCode.ADDITIONAL_PIPING, PriceCalculationType.PER_UNIT, "2000", null, 10),
                rule("DISCOUNT", PriceConditionCode.DISCOUNT, PriceCalculationType.PERCENTAGE, "10", null, 10));
        when(repository.lockPricingRequest(requestId))
                .thenReturn(Optional.of(new PricingRequestState("SUBMITTED", false, false)));
        when(repository.findPricingItems(requestId)).thenReturn(List.of(new PricingItem(
                "AIR_CONDITIONER", 1, new BigDecimal("10"), new BigDecimal("2"), new BigDecimal("3"))));
        when(repository.findEffectivePriceRules(any(OffsetDateTime.class))).thenReturn(rules);
        when(repository.nextQuoteVersion(requestId)).thenReturn(1);
        when(repository.findQuote(eq(requestId), any())).thenAnswer(invocation -> {
            UUID quoteId = invocation.getArgument(1);
            return Optional.of(new QuoteResponse(
                    quoteId, "QT-20261020-ABCDEF", 1, "ISSUED", "KRW",
                    new BigDecimal("117000.00"), new BigDecimal("11700.00"), new BigDecimal("10530.00"),
                    new BigDecimal("115830.00"), OffsetDateTime.now(), OffsetDateTime.now().plusDays(7), List.of()));
        });

        QuoteResponse response = pricingService.issueQuote(requestId, actorId);

        ArgumentCaptor<UUID> quoteId = ArgumentCaptor.forClass(UUID.class);
        verify(repository).insertQuote(quoteId.capture(), any(String.class), eq(requestId), eq(1),
                eq(new BigDecimal("117000.00")), eq(new BigDecimal("11700.00")),
                eq(new BigDecimal("10530.00")), eq(new BigDecimal("115830.00")),
                any(OffsetDateTime.class), any(OffsetDateTime.class), any(), eq(new BigDecimal("10.00")));
        ArgumentCaptor<QuoteLineDraft> lines = ArgumentCaptor.forClass(QuoteLineDraft.class);
        verify(repository, org.mockito.Mockito.times(5)).insertQuoteLine(eq(quoteId.getValue()), lines.capture());
        assertEquals(5, lines.getAllValues().size());
        assertEquals(1, lines.getAllValues().stream()
                .filter(line -> line.conditionCode() == PriceConditionCode.DISCOUNT).count());
        verify(repository).snapshotAppliedRules(quoteId.getValue());
        verify(repository).updateRequestAsQuoted(requestId);
        verify(repository).recordQuoteAudit(eq(actorId), eq(quoteId.getValue()), eq("QUOTE_ISSUED"),
                any(String.class), eq(response.versionNo()), eq(response.totalAmount()));
    }

    @Test
    void refusesToIssueQuoteWithoutApplicableBasePrice() {
        UUID requestId = UUID.randomUUID();
        when(repository.lockPricingRequest(requestId))
                .thenReturn(Optional.of(new PricingRequestState("SUBMITTED", false, false)));
        when(repository.findPricingItems(requestId)).thenReturn(List.of(new PricingItem(
                "AIR_CONDITIONER", 1, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
        when(repository.findEffectivePriceRules(any(OffsetDateTime.class))).thenReturn(List.of());

        assertThrows(PriceRuleNotConfiguredException.class, () -> pricingService.issueQuote(requestId, UUID.randomUUID()));
        verify(repository, never()).insertQuote(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void refusesToReissueQuoteAfterCustomerAcceptedIt() {
        UUID requestId = UUID.randomUUID();
        when(repository.lockPricingRequest(requestId))
                .thenReturn(Optional.of(new PricingRequestState("QUOTED", true, false)));

        assertThrows(QuoteNotAcceptableException.class, () -> pricingService.issueQuote(requestId, UUID.randomUUID()));
        verify(repository, never()).findEffectivePriceRules(any());
    }

    @Test
    void refusesInvalidRuleCalculationType() {
        PriceRuleRequest request = new PriceRuleRequest(
                "BAD_BASE", "Invalid base", null, null, PriceConditionCode.BASE,
                PriceCalculationType.PER_UNIT, new BigDecimal("1000"), 1, true, null, null);

        assertThrows(InvalidPriceRuleException.class, () -> pricingService.createPriceRule(request, UUID.randomUUID()));
        verify(repository, never()).createPriceRule(any(), any(), any());
    }

    @Test
    void acceptsOnlyOwnedCurrentUnexpiredQuote() {
        UUID quoteId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(repository.lockQuoteForAcceptance(quoteId, customerId)).thenReturn(Optional.of(
                new PricingRepository.QuoteAcceptanceState(
                        requestId, "ISSUED", OffsetDateTime.now().plusDays(1), "QUOTED", true)));

        pricingService.acceptQuote(quoteId, customerId);

        verify(repository).acceptQuote(quoteId, requestId);
        verify(repository).recordQuoteAudit(customerId, quoteId, "QUOTE_ACCEPTED", null, null, null);
    }

    @Test
    void refusesExpiredQuote() {
        UUID quoteId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(repository.lockQuoteForAcceptance(quoteId, customerId)).thenReturn(Optional.of(
                new PricingRepository.QuoteAcceptanceState(
                        UUID.randomUUID(), "ISSUED", OffsetDateTime.now().minusSeconds(1), "QUOTED", true)));

        assertThrows(QuoteNotAcceptableException.class,
                () -> pricingService.acceptQuote(quoteId, customerId));
        verify(repository, never()).acceptQuote(any(), any());
    }

    @Test
    void refusesToAcceptAnOlderQuoteVersion() {
        UUID quoteId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(repository.lockQuoteForAcceptance(quoteId, customerId)).thenReturn(Optional.of(
                new PricingRepository.QuoteAcceptanceState(
                        UUID.randomUUID(), "ISSUED", OffsetDateTime.now().plusDays(1), "QUOTED", false)));

        assertThrows(QuoteNotAcceptableException.class,
                () -> pricingService.acceptQuote(quoteId, customerId));
        verify(repository, never()).acceptQuote(any(), any());
    }

    private PriceRule rule(
            String code,
            PriceConditionCode condition,
            PriceCalculationType calculation,
            String amount,
            String applianceCode,
            int priority
    ) {
        return new PriceRule(UUID.randomUUID(), code, code, null, applianceCode, condition, calculation,
                new BigDecimal(amount), priority, true, 1, null, null);
    }
}
