import type { RestaurantCartGroup } from '../cart/cartStore';

/** Body of POST /api/orders: one order per restaurant. */
export interface OrderSubmission {
  restaurantId: number;
  items: { menuItemId: number; quantity: number; optionIds: number[] }[];
}

/** Server receipt for a placed order; total includes add-ons and is what was charged. */
export interface OrderSubmissionResult {
  orderId: number;
  restaurantId: number;
  status: string;
  total: number;
  placedAt: string;
  paymentReference: string;
}

export interface OrderCancellationResult {
  orderId: number;
  previousStatus: string;
  status: string;
  changedAt: string;
}

export function toOrderSubmission(group: RestaurantCartGroup): OrderSubmission {
  return {
    restaurantId: group.restaurantId,
    items: group.lines.map((line) => ({ menuItemId: line.itemId, quantity: line.quantity, optionIds: [...line.optionIds] })),
  };
}

async function postWithToken<T>(endpoint: string, token: string, body: unknown, failure: string, fetchImpl: typeof fetch): Promise<T> {
  const response = await fetchImpl(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    // Prefer the backend's explanation (closed restaurant, sold-out item, declined payment).
    const fieldError = data?.fieldErrors ? Object.values(data.fieldErrors)[0] : undefined;
    throw new Error(fieldError ?? data?.message ?? `${failure} (${response.status}).`);
  }
  return data as T;
}

export async function sendOrder(
  endpoint: string,
  token: string,
  order: OrderSubmission,
  fetchImpl: typeof fetch = fetch,
): Promise<OrderSubmissionResult> {
  const result = await postWithToken<OrderSubmissionResult>(endpoint, token, order, 'Order submission failed', fetchImpl);
  // BigDecimal arrives as a JSON number, but coerce in case a proxy stringifies it.
  return { ...result, total: Number(result.total) };
}

export async function sendOrderCancellation(
  endpoint: string,
  token: string,
  fetchImpl: typeof fetch = fetch,
): Promise<OrderCancellationResult> {
  return postWithToken<OrderCancellationResult>(endpoint, token, undefined, 'Order cancellation failed', fetchImpl);
}
