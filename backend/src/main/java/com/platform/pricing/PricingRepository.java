package com.platform.pricing;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
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

    private final JdbcTemplate jdbcTemplate;

    public PricingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PriceRule> findPriceRules() {
        return jdbcTemplate.query("""
                SELECT id, rule_code, name, description,
                       conditions ->> 'applianceTypeCode' AS appliance_type_code,
                       conditions ->> 'conditionCode' AS condition_code,
                       calculation_type, amount, priority, active, version, valid_from, valid_to
                FROM price_rule
                ORDER BY active DESC, priority DESC, rule_code
                """, this::mapPriceRule);
    }

    public Optional<PriceRule> findPriceRule(UUID id) {
        List<PriceRule> rules = jdbcTemplate.query("""
                SELECT id, rule_code, name, description,
                       conditions ->> 'applianceTypeCode' AS appliance_type_code,
                       conditions ->> 'conditionCode' AS condition_code,
                       calculation_type, amount, priority, active, version, valid_from, valid_to
                FROM price_rule
                WHERE id = ?
                """, this::mapPriceRule, id);
        return rules.stream().findFirst();
    }

    public PriceRule createPriceRule(UUID id, PriceRuleRequest request, UUID actorId) {
        try {
            return jdbcTemplate.queryForObject("""
                    INSERT INTO price_rule (
                        id, rule_code, name, description, conditions, calculation_type,
                        amount, priority, active, valid_from, valid_to, created_by
                    )
                    VALUES (?, ?, ?, ?, jsonb_build_object(
                        'applianceTypeCode', CAST(? AS text),
                        'conditionCode', CAST(? AS text)
                    ), ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id, rule_code, name, description,
                              conditions ->> 'applianceTypeCode' AS appliance_type_code,
                              conditions ->> 'conditionCode' AS condition_code,
                              calculation_type, amount, priority, active, version, valid_from, valid_to
                    """, this::mapPriceRule, id, request.ruleCode(), request.name(), request.description(),
                    request.applianceTypeCode(), request.conditionCode().name(),
                    request.calculationType().name(), request.amount(), request.priority(),
                    request.active(), request.validFrom(), request.validTo(), actorId);
        } catch (DuplicateKeyException exception) {
            throw new PriceRuleConflictException();
        }
    }

    public PriceRule updatePriceRule(UUID id, PriceRuleRequest request) {
        try {
            List<PriceRule> updated = jdbcTemplate.query("""
                    UPDATE price_rule
                    SET rule_code = ?, name = ?, description = ?,
                        conditions = jsonb_build_object(
                            'applianceTypeCode', CAST(? AS text),
                            'conditionCode', CAST(? AS text)
                        ),
                        calculation_type = ?, amount = ?, priority = ?, active = ?,
                        valid_from = ?, valid_to = ?, version = version + 1, updated_at = now()
                    WHERE id = ?
                    RETURNING id, rule_code, name, description,
                              conditions ->> 'applianceTypeCode' AS appliance_type_code,
                              conditions ->> 'conditionCode' AS condition_code,
                              calculation_type, amount, priority, active, version, valid_from, valid_to
                    """, this::mapPriceRule, request.ruleCode(), request.name(), request.description(),
                    request.applianceTypeCode(), request.conditionCode().name(),
                    request.calculationType().name(), request.amount(), request.priority(),
                    request.active(), request.validFrom(), request.validTo(), id);
            if (updated.isEmpty()) {
                throw new PriceRuleNotFoundException();
            }
            return updated.getFirst();
        } catch (DuplicateKeyException exception) {
            throw new PriceRuleConflictException();
        }
    }

    public void recordPriceRuleAudit(UUID actorId, String action, PriceRule after, PriceRule before) {
        String beforeSummary = before == null ? null : """
                {"ruleCode":"%s","version":%d,"active":%s,"amount":%s}
                """.formatted(before.ruleCode(), before.version(), before.active(),
                before.amount().toPlainString()).strip();
        jdbcTemplate.update("""
                INSERT INTO audit_log (
                    id, actor_user_id, action, entity_type, entity_id, before_summary, after_summary
                )
                VALUES (?, ?, ?, 'PRICE_RULE', ?, CAST(? AS jsonb), jsonb_build_object(
                    'ruleCode', ?, 'version', ?, 'active', ?, 'amount', ?, 'conditionCode', ?
                ))
                """, UUID.randomUUID(), actorId, action, after.id(), beforeSummary,
                after.ruleCode(), after.version(), after.active(), after.amount(),
                after.conditionCode().name());
    }

    public void recordQuoteAudit(
            UUID actorId,
            UUID quoteId,
            String action,
            String quoteNo,
            Integer version,
            BigDecimal total
    ) {
        jdbcTemplate.update("""
                INSERT INTO audit_log (id, actor_user_id, action, entity_type, entity_id, after_summary)
                VALUES (?, ?, ?, 'QUOTE', ?, jsonb_build_object(
                    'quoteNo', CAST(? AS text), 'version', ?, 'totalAmount', ?
                ))
                """, UUID.randomUUID(), actorId, action, quoteId, quoteNo, version, total);
    }

    public List<PriceRule> findEffectivePriceRules(OffsetDateTime now) {
        return jdbcTemplate.query("""
                SELECT id, rule_code, name, description,
                       conditions ->> 'applianceTypeCode' AS appliance_type_code,
                       conditions ->> 'conditionCode' AS condition_code,
                       calculation_type, amount, priority, active, version, valid_from, valid_to
                FROM price_rule
                WHERE active = true
                  AND (valid_from IS NULL OR valid_from <= ?)
                  AND (valid_to IS NULL OR valid_to > ?)
                ORDER BY priority DESC, rule_code
                FOR SHARE
                """, this::mapPriceRule, now, now);
    }

    public Optional<PricingRequestState> lockPricingRequest(UUID requestId) {
        return jdbcTemplate.query("""
                SELECT mr.status,
                       EXISTS (SELECT 1 FROM quote q
                               WHERE q.move_request_id = mr.id AND q.status = 'ACCEPTED') AS quote_accepted,
                       EXISTS (SELECT 1 FROM reservation r WHERE r.move_request_id = mr.id) AS has_reservation
                FROM move_request mr
                WHERE mr.id = ?
                FOR UPDATE OF mr
                """, resultSet -> resultSet.next()
                ? Optional.of(new PricingRequestState(
                    resultSet.getString("status"),
                    resultSet.getBoolean("quote_accepted"),
                    resultSet.getBoolean("has_reservation")))
                : Optional.empty(), requestId);
    }

    public List<PricingItem> findPricingItems(UUID requestId) {
        return jdbcTemplate.query("""
                SELECT appliance_type.code AS appliance_type_code, mi.quantity,
                       COALESCE((mi.installation_conditions ->> 'carryingDistanceMeters')::numeric, 0)
                           AS carrying_distance_meters,
                       COALESCE((mi.installation_conditions ->> 'floorCount')::numeric, 0) AS floor_count,
                       COALESCE((mi.installation_conditions ->> 'additionalPipingMeters')::numeric, 0)
                           AS additional_piping_meters
                FROM move_item mi
                JOIN appliance_type ON appliance_type.id = mi.appliance_type_id
                WHERE mi.move_request_id = ?
                ORDER BY mi.created_at, mi.id
                """, (resultSet, rowNumber) -> new PricingItem(
                resultSet.getString("appliance_type_code"),
                resultSet.getInt("quantity"),
                resultSet.getBigDecimal("carrying_distance_meters"),
                resultSet.getBigDecimal("floor_count"),
                resultSet.getBigDecimal("additional_piping_meters")
        ), requestId);
    }

    public long countPricingRequests() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM move_request mr
                WHERE mr.status IN ('SUBMITTED', 'QUOTED')
                  AND NOT EXISTS (
                      SELECT 1 FROM quote accepted
                      WHERE accepted.move_request_id = mr.id AND accepted.status = 'ACCEPTED'
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM reservation r WHERE r.move_request_id = mr.id
                  )
                """, Long.class);
        return count == null ? 0 : count;
    }

    public List<PricingRequestSummary> findPricingRequests(int limit, int offset) {
        return jdbcTemplate.query("""
                SELECT mr.id, mr.request_no, mr.status, customer.name AS customer_name,
                       customer.email AS customer_email, appliance_type.code AS appliance_type_code,
                       appliance_type.name AS appliance_type_name, mr.origin_address,
                       mr.destination_address, mr.desired_start_at, mr.created_at,
                       latest.status AS latest_quote_status, latest.total_amount AS latest_quote_total_amount
                FROM move_request mr
                JOIN app_user customer ON customer.id = mr.customer_id
                JOIN move_item mi ON mi.move_request_id = mr.id
                JOIN appliance_type ON appliance_type.id = mi.appliance_type_id
                LEFT JOIN LATERAL (
                    SELECT q.status, q.total_amount
                    FROM quote q
                    WHERE q.move_request_id = mr.id
                    ORDER BY q.version_no DESC
                    LIMIT 1
                ) latest ON true
                WHERE mr.status IN ('SUBMITTED', 'QUOTED')
                  AND NOT EXISTS (
                      SELECT 1 FROM quote accepted
                      WHERE accepted.move_request_id = mr.id AND accepted.status = 'ACCEPTED'
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM reservation r WHERE r.move_request_id = mr.id
                  )
                ORDER BY mr.created_at, mr.id
                LIMIT ? OFFSET ?
                """, (resultSet, rowNumber) -> new PricingRequestSummary(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("request_no"),
                resultSet.getString("status"),
                resultSet.getString("customer_name"),
                resultSet.getString("customer_email"),
                resultSet.getString("appliance_type_code"),
                resultSet.getString("appliance_type_name"),
                resultSet.getString("origin_address"),
                resultSet.getString("destination_address"),
                resultSet.getObject("desired_start_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getString("latest_quote_status"),
                resultSet.getBigDecimal("latest_quote_total_amount")
        ), limit, offset);
    }

    public int nextQuoteVersion(UUID requestId) {
        Integer version = jdbcTemplate.queryForObject("""
                SELECT COALESCE(max(version_no), 0) + 1
                FROM quote
                WHERE move_request_id = ?
                """, Integer.class, requestId);
        return version == null ? 1 : version;
    }

    public void expireIssuedQuotes(UUID requestId) {
        jdbcTemplate.update("""
                UPDATE quote
                SET status = 'EXPIRED', updated_at = now()
                WHERE move_request_id = ? AND status = 'ISSUED'
                """, requestId);
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
        jdbcTemplate.update("""
                INSERT INTO quote (
                    id, quote_no, move_request_id, version_no, status,
                    subtotal_amount, discount_amount, tax_amount, total_amount,
                    currency, calculation_snapshot, issued_at, expires_at
                )
                VALUES (?, ?, ?, ?, 'ISSUED', ?, ?, ?, ?, 'KRW',
                        jsonb_build_object(
                            'carryingDistanceMeters', ?,
                            'floorCount', ?,
                            'additionalPipingMeters', ?,
                            'taxRatePercent', ?
                        ), ?, ?)
                """, id, quoteNo, requestId, version, subtotal, discount, tax, total,
                conditions.carryingDistanceMeters(), conditions.floorCount(),
                conditions.additionalPipingMeters(), taxRate, issuedAt, expiresAt);
    }

    public void insertQuoteLine(UUID quoteId, QuoteLineDraft line) {
        jdbcTemplate.update("""
                INSERT INTO quote_line (
                    id, quote_id, line_code, description, quantity, unit_price, line_amount, rule_snapshot
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, jsonb_build_object(
                    'ruleId', ?,
                    'ruleCode', ?,
                    'version', ?,
                    'conditionCode', ?,
                    'calculationType', ?,
                    'configuredAmount', ?,
                    'applianceTypeCode', CAST(? AS text)
                ))
                """, UUID.randomUUID(), quoteId, line.lineCode(), line.description(), line.quantity(),
                line.unitPrice(), line.lineAmount(), line.ruleId(), line.lineCode(), line.ruleVersion(),
                line.conditionCode().name(), line.calculationType().name(), line.configuredAmount(),
                line.applianceTypeCode());
    }

    public void snapshotAppliedRules(UUID quoteId) {
        jdbcTemplate.update("""
                UPDATE quote
                SET calculation_snapshot = calculation_snapshot || jsonb_build_object(
                    'appliedRules',
                    COALESCE((
                        SELECT jsonb_agg(rule_snapshot ORDER BY id)
                        FROM quote_line
                        WHERE quote_id = ?
                    ), '[]'::jsonb)
                )
                WHERE id = ?
                """, quoteId, quoteId);
    }

    public void updateRequestAsQuoted(UUID requestId) {
        jdbcTemplate.update("""
                UPDATE move_request
                SET status = 'QUOTED', updated_at = now()
                WHERE id = ?
                """, requestId);
    }

    public boolean ownsRequest(UUID requestId, UUID customerId) {
        Boolean owns = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM move_request
                    WHERE id = ? AND customer_id = ?
                )
                """, Boolean.class, requestId, customerId);
        return Boolean.TRUE.equals(owns);
    }

    public List<QuoteResponse> findQuotes(UUID requestId, int limit, int offset) {
        List<QuoteHeader> headers = jdbcTemplate.query("""
                SELECT id, quote_no, version_no, status, currency, subtotal_amount,
                       discount_amount, tax_amount, total_amount, issued_at, expires_at
                FROM quote
                WHERE move_request_id = ?
                ORDER BY version_no DESC
                LIMIT ? OFFSET ?
                """, (resultSet, rowNumber) -> new QuoteHeader(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("quote_no"),
                resultSet.getInt("version_no"),
                resultSet.getString("status"),
                resultSet.getString("currency"),
                resultSet.getBigDecimal("subtotal_amount"),
                resultSet.getBigDecimal("discount_amount"),
                resultSet.getBigDecimal("tax_amount"),
                resultSet.getBigDecimal("total_amount"),
                resultSet.getObject("issued_at", OffsetDateTime.class),
                resultSet.getObject("expires_at", OffsetDateTime.class)
        ), requestId, limit, offset);

        if (headers.isEmpty()) {
            return List.of();
        }
        List<UUID> quoteIds = headers.stream().map(QuoteHeader::id).toList();
        String placeholders = String.join(",", java.util.Collections.nCopies(quoteIds.size(), "?"));
        Map<UUID, List<QuoteLineResponse>> linesByQuote = new HashMap<>();
        jdbcTemplate.query("""
                SELECT quote_id, line_code, description, quantity, unit_price, line_amount
                FROM quote_line
                WHERE quote_id IN (%s)
                ORDER BY quote_id, id
                """.formatted(placeholders), (RowCallbackHandler) resultSet -> {
            while (resultSet.next()) {
                UUID quoteId = resultSet.getObject("quote_id", UUID.class);
                linesByQuote.computeIfAbsent(quoteId, ignored -> new ArrayList<>()).add(
                        new QuoteLineResponse(resultSet.getString("line_code"),
                                resultSet.getString("description"),
                                resultSet.getBigDecimal("quantity"),
                                resultSet.getBigDecimal("unit_price"),
                                resultSet.getBigDecimal("line_amount")));
            }
        }, quoteIds.toArray(new Object[0]));

        return headers.stream().map(header -> new QuoteResponse(
                header.id(), header.quoteNo(), header.versionNo(), header.status(), header.currency(),
                header.subtotalAmount(), header.discountAmount(), header.taxAmount(), header.totalAmount(),
                header.issuedAt(), header.expiresAt(), linesByQuote.getOrDefault(header.id(), List.of())
        )).toList();
    }

    public long countQuotes(UUID requestId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM quote WHERE move_request_id = ?
                """, Long.class, requestId);
        return count == null ? 0 : count;
    }

    public Optional<QuoteResponse> findQuote(UUID requestId, UUID quoteId) {
        List<QuoteHeader> quotes = jdbcTemplate.query("""
                SELECT id, quote_no, version_no, status, currency, subtotal_amount,
                       discount_amount, tax_amount, total_amount, issued_at, expires_at
                FROM quote
                WHERE id = ? AND move_request_id = ?
                """, (resultSet, rowNumber) -> new QuoteHeader(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("quote_no"),
                resultSet.getInt("version_no"),
                resultSet.getString("status"),
                resultSet.getString("currency"),
                resultSet.getBigDecimal("subtotal_amount"),
                resultSet.getBigDecimal("discount_amount"),
                resultSet.getBigDecimal("tax_amount"),
                resultSet.getBigDecimal("total_amount"),
                resultSet.getObject("issued_at", OffsetDateTime.class),
                resultSet.getObject("expires_at", OffsetDateTime.class)
        ), quoteId, requestId);
        return quotes.stream().findFirst().map(header -> new QuoteResponse(
                header.id(), header.quoteNo(), header.versionNo(), header.status(), header.currency(),
                header.subtotalAmount(), header.discountAmount(), header.taxAmount(), header.totalAmount(),
                header.issuedAt(), header.expiresAt(), findQuoteLines(quoteId)));
    }

    private List<QuoteLineResponse> findQuoteLines(UUID quoteId) {
        return jdbcTemplate.query("""
                SELECT line_code, description, quantity, unit_price, line_amount
                FROM quote_line
                WHERE quote_id = ?
                ORDER BY id
                """, (resultSet, rowNumber) -> new QuoteLineResponse(
                resultSet.getString("line_code"),
                resultSet.getString("description"),
                resultSet.getBigDecimal("quantity"),
                resultSet.getBigDecimal("unit_price"),
                resultSet.getBigDecimal("line_amount")
        ), quoteId);
    }

    public Optional<QuoteAcceptanceState> lockQuoteForAcceptance(UUID quoteId, UUID customerId) {
        return jdbcTemplate.query("""
                SELECT mr.id AS request_id, q.status, q.expires_at, mr.status AS request_status,
                       q.version_no = (SELECT max(latest.version_no)
                                       FROM quote latest
                                       WHERE latest.move_request_id = q.move_request_id) AS is_latest
                FROM quote q
                JOIN move_request mr ON mr.id = q.move_request_id
                WHERE q.id = ? AND mr.customer_id = ?
                FOR UPDATE OF mr, q
                """, resultSet -> resultSet.next()
                ? Optional.of(new QuoteAcceptanceState(
                    resultSet.getObject("request_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getObject("expires_at", OffsetDateTime.class),
                    resultSet.getString("request_status"),
                    resultSet.getBoolean("is_latest")))
                : Optional.empty(), quoteId, customerId);
    }

    public void acceptQuote(UUID quoteId, UUID requestId) {
        jdbcTemplate.update("""
                UPDATE quote
                SET status = 'EXPIRED', updated_at = now()
                WHERE move_request_id = ? AND status = 'ISSUED' AND id <> ?
                """, requestId, quoteId);
        jdbcTemplate.update("""
                UPDATE quote SET status = 'ACCEPTED', updated_at = now() WHERE id = ?
                """, quoteId);
    }

    private PriceRule mapPriceRule(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        return new PriceRule(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("rule_code"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getString("appliance_type_code"),
                PriceConditionCode.valueOf(resultSet.getString("condition_code")),
                PriceCalculationType.valueOf(resultSet.getString("calculation_type")),
                resultSet.getBigDecimal("amount"),
                resultSet.getInt("priority"),
                resultSet.getBoolean("active"),
                resultSet.getInt("version"),
                resultSet.getObject("valid_from", OffsetDateTime.class),
                resultSet.getObject("valid_to", OffsetDateTime.class)
        );
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

    private record QuoteHeader(
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
}
