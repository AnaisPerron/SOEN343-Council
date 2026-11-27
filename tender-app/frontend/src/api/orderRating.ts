import { requestJson } from './client.ts';

export interface OrderRatingView {
  orderId: number;
  stars: number | null;
  restaurantStars: number | null;
  courierStars: number | null;
  comment: string | null;
  createdAt: string;
}

export function fetchOrderRating(orderId: number, token: string): Promise<OrderRatingView | null> {
  return requestJson<OrderRatingView | null>('GET', `/api/orders/${orderId}/rating`, undefined, token);
}
