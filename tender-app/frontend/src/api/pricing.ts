import { requestJson } from './client';

export type PricingRules = {
  deliveryFeePerKm: number;
  serviceFee: number;
  smallOrderThreshold: number;
  smallOrderSurcharge: number;
};

export function fetchPricingRules(token: string): Promise<PricingRules> {
  return requestJson<PricingRules>('GET', '/api/pricing/rules', undefined, token);
}
