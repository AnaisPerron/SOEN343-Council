export const ORDER_STATUSES = [
  'placed', 'accepted', 'preparing', 'ready', 'picked_up',
  'delivered', 'rejected', 'cancelled', 'refunded',
] as const;

export type RestaurantOrderStatus = typeof ORDER_STATUSES[number];
export type OrderFilter = RestaurantOrderStatus | 'all';

export type RestaurantOrderLine = {
  name: string;
  quantity: number;
  unitPrice: number;
  toppings: { name: string; additionalCost: number }[];
};

export type RestaurantOrder = {
  id: number;
  restaurantId: number;
  status: RestaurantOrderStatus;
  createdAt: string;
  acceptedAt: string | null;
  updatedAt: string | null;
  rejectionReason: string | null;
  customer: { name: string; address: string; phone: string };
  lines: RestaurantOrderLine[];
  totalPrice?: number | null;
};

const STATUS_PRIORITY: Record<RestaurantOrderStatus, number> = {
  placed: 0, accepted: 1, preparing: 2, ready: 3, picked_up: 4,
  delivered: 5, rejected: 6, cancelled: 7, refunded: 8,
};

export function filterAndSortOrders(orders: RestaurantOrder[], filter: OrderFilter): RestaurantOrder[] {
  return orders
    .filter((order) => filter === 'all' || order.status === filter)
    .sort((a, b) => {
      if (filter === 'all' && STATUS_PRIORITY[a.status] !== STATUS_PRIORITY[b.status]) {
        return STATUS_PRIORITY[a.status] - STATUS_PRIORITY[b.status];
      }
      return Date.parse(a.createdAt) - Date.parse(b.createdAt) || a.id - b.id;
    });
}

export function transitionOrder(order: RestaurantOrder, next: RestaurantOrderStatus, timestamp: string, reason?: string): RestaurantOrder {
  const allowed = (order.status === 'placed' && (next === 'accepted' || next === 'rejected'))
    || (order.status === 'accepted' && next === 'preparing')
    || (order.status === 'preparing' && next === 'ready')
    || (order.status === 'ready' && next === 'picked_up')
    || (order.status === 'picked_up' && next === 'delivered');
  if (!allowed) throw new Error(`Cannot change ${order.status} to ${next}.`);
  return {
    ...order,
    status: next,
    updatedAt: timestamp,
    acceptedAt: next === 'accepted' ? timestamp : order.acceptedAt,
    rejectionReason: next === 'rejected' ? reason?.trim() || null : order.rejectionReason,
  };
}

export function orderTotal(order: RestaurantOrder): number {
  if (order.totalPrice != null) return order.totalPrice;
  return order.lines.reduce((total, line) =>
    total + line.quantity * (line.unitPrice + line.toppings.reduce((extras, topping) => extras + topping.additionalCost, 0)), 0);
}

export function deleteSelectedOrders(orders: RestaurantOrder[], selectedIds: ReadonlySet<number>): RestaurantOrder[] {
  return orders.filter((order) => !selectedIds.has(order.id));
}
