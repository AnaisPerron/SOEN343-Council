import { requestJson } from './client';

export interface OrderSummary {
  orderId: number;
  status: string | null;
  restaurantName: string | null;
  finishedAt: string | null;
  orderItems: {
    menuItemId: number;
    name: string;
    quantity: number;
    unitPrice: number;
    lineTotal: number;
  }[];
  charges: { chargeType: string; amount: number }[];
  totalCharge: number | null;
}

export function fetchCustomerOrderSummary(orderId: string, token: string): Promise<OrderSummary> {
  return requestJson<OrderSummary>('GET', `/api/order-summary/customer/${encodeURIComponent(orderId)}`, undefined, token);
}
