export interface IncomingOrderLine {
  itemName: string;
  quantity: number;
  unitPrice: number;
}

export interface IncomingOrder {
  id: number;
  status: string;
  dateOrdered: string | null;
  acceptedAt: string | null;
  rejectionReason: string | null;
  totalPrice: number | null;
  orderLines: IncomingOrderLine[];
}

function isIncomingOrder(value: unknown): value is IncomingOrder {
  if (typeof value !== 'object' || value === null) {
    return false;
  }

  const order = value as Record<string, unknown>;
  return (
    typeof order.id === 'number' &&
    typeof order.status === 'string' &&
    (typeof order.dateOrdered === 'string' || order.dateOrdered === null) &&
    (typeof order.acceptedAt === 'string' || order.acceptedAt === null) &&
    (typeof order.rejectionReason === 'string' || order.rejectionReason === null) &&
    (typeof order.totalPrice === 'number' || order.totalPrice === null) &&
    Array.isArray(order.orderLines) &&
    order.orderLines.every(
      (item: unknown) =>
        typeof item === 'object' &&
        item !== null &&
        typeof (item as Record<string, unknown>).itemName === 'string' &&
        typeof (item as Record<string, unknown>).quantity === 'number' &&
        typeof (item as Record<string, unknown>).unitPrice === 'number',
    )
  );
}

function getApiBaseUrl(): string {
  return (process.env.EXPO_PUBLIC_API_URL?.trim() || 'http://localhost:8080').replace(/\/+$/, '');
}

export async function fetchOrders(
  restaurantId: string,
  queue: 'incoming' | 'preparation' | 'pickup',
): Promise<IncomingOrder[]> {
  const response = await fetch(
    `${getApiBaseUrl()}/api/restaurants/${encodeURIComponent(restaurantId)}/orders/${queue}`,
  );
  if (!response.ok) {
    throw new Error(`Unable to load incoming orders (HTTP ${response.status}).`);
  }

  const payload: unknown = await response.json();
  if (!Array.isArray(payload) || !payload.every(isIncomingOrder)) {
    throw new Error('The server returned an invalid incoming-orders response.');
  }

  return payload;
}

export type OrderAction = 'accept' | 'reject' | 'cancel' | 'prepare' | 'ready';

export async function transitionOrder(
  restaurantId: string,
  orderId: number,
  action: OrderAction,
  reason?: string,
): Promise<void> {
  const response = await fetch(
    `${getApiBaseUrl()}/api/restaurants/${encodeURIComponent(restaurantId)}/orders/${orderId}/${action}`,
    {
      method: 'POST',
      headers: action === 'reject' ? { 'Content-Type': 'application/json' } : undefined,
      body: action === 'reject' ? JSON.stringify({ reason }) : undefined,
    },
  );
  if (!response.ok) {
    const detail = await response.text();
    throw new Error(detail || `Unable to ${action} order (HTTP ${response.status}).`);
  }
}

export function observeOrderStatus(
  restaurantId: string,
  onStatusChange: () => void,
): () => void {
  const events = new EventSource(
    `${getApiBaseUrl()}/api/restaurants/${encodeURIComponent(restaurantId)}/orders/events`,
  );
  events.onmessage = onStatusChange;
  return () => events.close();
}
