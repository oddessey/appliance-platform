package com.platform.pricing;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping("/admin/price-rules")
    public List<PriceRuleResponse> listPriceRules() {
        return pricingService.listPriceRules();
    }

    @PostMapping("/admin/price-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public PriceRuleResponse createPriceRule(
            @Valid @RequestBody PriceRuleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return pricingService.createPriceRule(request, UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping("/admin/price-rules/{id}")
    public PriceRuleResponse updatePriceRule(
            @PathVariable UUID id,
            @Valid @RequestBody PriceRuleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return pricingService.updatePriceRule(id, request, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/admin/pricing/requests")
    public PricingRequestPageResponse listPricingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return pricingService.listPricingRequests(page, size);
    }

    @PostMapping("/admin/pricing/requests/{id}/quotes")
    public QuoteResponse issueQuote(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return pricingService.issueQuote(id, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/installations/{id}/quotes")
    public QuotePageResponse listCustomerQuotes(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return pricingService.listCustomerQuotes(id, UUID.fromString(jwt.getSubject()), page, size);
    }

    @PostMapping("/quotes/{quoteId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptQuote(@PathVariable UUID quoteId, @AuthenticationPrincipal Jwt jwt) {
        pricingService.acceptQuote(quoteId, UUID.fromString(jwt.getSubject()));
    }
}
