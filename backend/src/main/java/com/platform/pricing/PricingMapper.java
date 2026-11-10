package com.platform.pricing;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface PricingMapper {

    List<PriceRule> findPriceRules();

    PriceRule findPriceRule(@Param("id") UUID id);

    PriceRule createPriceRule(
            @Param("id") UUID id,
            @Param("request") PriceRuleRequest request,
            @Param("actorId") UUID actorId
    );

    PriceRule updatePriceRule(@Param("id") UUID id, @Param("request") PriceRuleRequest request);

    void recordPriceRuleAudit(
            @Param("id") UUID id,
            @Param("actorId") UUID actorId,
            @Param("action") String action,
            @Param("entityId") UUID entityId,
            @Param("beforeSummary") String beforeSummary,
            @Param("after") PriceRule after
    );

    void recordQuoteAudit(
            @Param("id") UUID id,
            @Param("actorId") UUID actorId,
            @Param("action") String action,
            @Param("quoteId") UUID quoteId,
            @Param("quoteNo") String quoteNo,
            @Param("version") Integer version,
            @Param("total") BigDecimal total
    );

    List<PriceRule> findEffectivePriceRules(@Param("now") OffsetDateTime now);

    PricingRepository.PricingRequestState lockPricingRequest(@Param("requestId") UUID requestId);

    List<PricingRepository.PricingItem> findPricingItems(@Param("requestId") UUID requestId);

    long countPricingRequests();

    List<PricingRequestSummary> findPricingRequests(
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    Integer nextQuoteVersion(@Param("requestId") UUID requestId);

    void expireIssuedQuotes(@Param("requestId") UUID requestId);

    void insertQuote(
            @Param("id") UUID id,
            @Param("quoteNo") String quoteNo,
            @Param("requestId") UUID requestId,
            @Param("version") int version,
            @Param("subtotal") BigDecimal subtotal,
            @Param("discount") BigDecimal discount,
            @Param("tax") BigDecimal tax,
            @Param("total") BigDecimal total,
            @Param("conditions") PricingRepository.PricingConditions conditions,
            @Param("taxRate") BigDecimal taxRate,
            @Param("issuedAt") OffsetDateTime issuedAt,
            @Param("expiresAt") OffsetDateTime expiresAt
    );

    void insertQuoteLine(
            @Param("id") UUID id,
            @Param("quoteId") UUID quoteId,
            @Param("line") PricingRepository.QuoteLineDraft line
    );

    void snapshotAppliedRules(@Param("quoteId") UUID quoteId);

    void updateRequestAsQuoted(@Param("requestId") UUID requestId);

    Boolean ownsRequest(@Param("requestId") UUID requestId, @Param("customerId") UUID customerId);

    List<QuoteHeader> findQuotes(
            @Param("requestId") UUID requestId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countQuotes(@Param("requestId") UUID requestId);

    QuoteHeader findQuote(@Param("requestId") UUID requestId, @Param("quoteId") UUID quoteId);

    List<QuoteLineWithQuoteId> findQuoteLines(@Param("quoteIds") List<UUID> quoteIds);

    List<QuoteLineResponse> findQuoteLinesForQuote(@Param("quoteId") UUID quoteId);

    PricingRepository.QuoteAcceptanceState lockQuoteForAcceptance(
            @Param("quoteId") UUID quoteId,
            @Param("customerId") UUID customerId
    );

    void expireOtherIssuedQuotes(@Param("requestId") UUID requestId, @Param("quoteId") UUID quoteId);

    void acceptQuote(@Param("quoteId") UUID quoteId);

    record QuoteHeader(
            UUID id,
            String quoteNo,
            int versionNo,
            String status,
            String currency,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt
    ) {
    }

    record QuoteLineWithQuoteId(
            UUID quoteId,
            String lineCode,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineAmount
    ) {
    }
}
