import { apiRequest } from "./client";
import type {
  PriceRule,
  PriceRuleRequest,
  PricingRequestPage,
  Quote,
  QuotePage,
} from "../types/installation";

export function getPriceRules(): Promise<PriceRule[]> {
  return apiRequest<PriceRule[]>("/admin/price-rules");
}

export function createPriceRule(request: PriceRuleRequest): Promise<PriceRule> {
  return apiRequest<PriceRule>("/admin/price-rules", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function updatePriceRule(id: string, request: PriceRuleRequest): Promise<PriceRule> {
  return apiRequest<PriceRule>(`/admin/price-rules/${encodeURIComponent(id)}`, {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}

export function getPricingRequests(page = 0, size = 20): Promise<PricingRequestPage> {
  return apiRequest<PricingRequestPage>(`/admin/pricing/requests?page=${page}&size=${size}`);
}

export function issueQuote(requestId: string): Promise<Quote> {
  return apiRequest<Quote>(`/admin/pricing/requests/${encodeURIComponent(requestId)}/quotes`, {
    method: "POST",
  });
}

export function getCustomerQuotes(requestId: string, page = 0, size = 20): Promise<QuotePage> {
  return apiRequest<QuotePage>(
    `/installations/${encodeURIComponent(requestId)}/quotes?page=${page}&size=${size}`,
  );
}

export function acceptQuote(quoteId: string): Promise<void> {
  return apiRequest<void>(`/quotes/${encodeURIComponent(quoteId)}/accept`, {
    method: "POST",
  });
}
