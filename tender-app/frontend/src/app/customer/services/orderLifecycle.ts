import type { RestaurantCartGroup } from '../cart/cartStore';

export type CustomerOrderStatus = 'placed' | 'cancelled' | 'accepted' | 'rejected' | 'preparing' | 'ready' | 'picked_up' | 'delivered' | 'refunded';

/** A line as the backend recorded it; unitPrice includes the add-ons. */
export interface CustomerOrderItem {
  name: string;
  quantity: number;
  unitPrice: number;
  options: string[];
}

export interface CustomerOrder {
  id: string;
  restaurantId: number;
  // Cart lines exist only for orders placed in this session; history from the backend uses items instead.
  lines: RestaurantCartGroup['lines'];
  items?: CustomerOrderItem[];
  restaurantName?: string;
  rejectionReason?: string | null;
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

/** Shape of one GET /api/orders entry. */
export interface OrderHistoryEntry {
  orderId: number;
  restaurantId: number;
  restaurantName: string;
  status: string;
  total: number;
  placedAt: string | null;
  rejectionReason: string | null;
  lines: CustomerOrderItem[];
}

export function fromOrderHistory(entry: OrderHistoryEntry): CustomerOrder {
  return {
    id: String(entry.orderId),
    restaurantId: entry.restaurantId,
    restaurantName: entry.restaurantName,
    lines: [],
    // BigDecimal arrives as a JSON number, but coerce in case a proxy stringifies it.
    items: entry.lines.map((line) => ({ ...line, unitPrice: Number(line.unitPrice), options: [...line.options] })),
    rejectionReason: entry.rejectionReason,
    total: Number(entry.total),
    status: entry.status as CustomerOrderStatus,
    createdAt: entry.placedAt ?? '',
  };
}

/** "picked_up" -> "Picked up" */
export function formatOrderStatus(status: CustomerOrderStatus): string {
  const words = status.replace(/_/g, ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}

export function cancelPlacedOrder(order: CustomerOrder): CustomerOrder {
  return order.status === 'placed' ? { ...order, status: 'cancelled' } : order;
}
