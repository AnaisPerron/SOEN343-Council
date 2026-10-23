export interface OrderSubmission {
  restaurantId: number;
  lineItems: { menuItemId: number; quantity: number; optionIds: number[] }[];
  paymentChoice: string;
}

export interface OrderSubmissionResult {
  orderId: number;
  status: string;
}

export async function sendOrder(
  endpoint: string,
  order: OrderSubmission,
  fetchImpl: typeof fetch = fetch,
): Promise<OrderSubmissionResult> {
  const response = await fetchImpl(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(order),
  });

  if (!response.ok) throw new Error(`Order submission failed (${response.status}).`);
  return response.json() as Promise<OrderSubmissionResult>;
}
