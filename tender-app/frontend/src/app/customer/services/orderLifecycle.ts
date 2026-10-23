import type { RestaurantCartGroup } from '../cart/cartStore';

export type CustomerOrderStatus = 'placed' | 'cancelled' | 'accepted' | 'rejected' | 'preparing' | 'ready';

export interface CustomerOrder {
  id: string;
  restaurantId: number;
  lines: RestaurantCartGroup['lines'];
  total: number;
  status: CustomerOrderStatus;
  createdAt: string;
}

export function createPlacedOrders(groups: RestaurantCartGroup[], timestamp: string, firstId: number): CustomerOrder[] {
  return groups.map((group, index) => ({
    id: String(firstId + index),
    restaurantId: group.restaurantId,
    lines: group.lines.map((line) => ({ ...line, optionIds: [...line.optionIds] })),
    total: group.total,
    status: 'placed',
    createdAt: timestamp,
  }));
}

export function cancelPlacedOrder(order: CustomerOrder): CustomerOrder {
  return order.status === 'placed' ? { ...order, status: 'cancelled' } : order;
}
