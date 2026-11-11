import { requestJson } from './client';

export interface OrderSummary {
  orderId: number;
  status: string | null;
  restaurantName: string | null;
  placedAt: string | null;
  acceptedAt: string | null;
  pickedUpAt: string | null;
  deliveredAt: string | null;
  cancelledAt: string | null;
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
  itemSubtotal: number | null;
}

export function fetchCustomerOrderSummary(orderId: string, token: string): Promise<OrderSummary> {
  return requestJson<OrderSummary>('GET', `/api/order-summary/customer/${encodeURIComponent(orderId)}`, undefined, token);
}
