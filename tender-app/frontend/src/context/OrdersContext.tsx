import { createContext, useContext, useState, type ReactNode } from 'react';
import type { RestaurantCartGroup } from '../app/customer/cart/cartStore';
import type { CustomerOrder } from '../app/customer/services/orderLifecycle';
import { requestJson } from '../api/client';
import { useAuth } from './AuthContext';

interface OrdersContextValue {
  orders: CustomerOrder[];
  placeOrder: (group: RestaurantCartGroup) => Promise<CustomerOrder>;
  cancelOrder: (orderId: string) => Promise<void>;
}

const OrdersContext = createContext<OrdersContextValue | null>(null);

export function OrdersProvider({ children }: { children: ReactNode }) {
  const [orders, setOrders] = useState<CustomerOrder[]>([]);
  const { token } = useAuth();

  const placeOrder = async (group: RestaurantCartGroup) => {
    if (!token) throw new Error('Please sign in to place an order.');
    if (group.lines.some((line) => line.optionIds.length > 0)) throw new Error('Ordering items with options is not supported yet. Remove the options before ordering.');
    const result = await requestJson<{ orderId: number; restaurantId: number; total: number; placedAt: string; status: string }>('POST', '/api/orders', {
      restaurantId: group.restaurantId,
      items: group.lines.map((line) => ({ menuItemId: line.itemId, quantity: line.quantity })),
    }, token);
    const placed: CustomerOrder = { id: String(result.orderId), restaurantId: result.restaurantId, lines: group.lines, total: Number(result.total), status: 'placed', createdAt: result.placedAt };
    setOrders((current) => [placed, ...current]);
    return placed;
  };

  const cancelOrder = async (orderId: string) => {
    if (!token) throw new Error('Please sign in to cancel an order.');
    await requestJson('POST', `/api/orders/${orderId}/cancel`, undefined, token);
    setOrders((current) => current.map((order) => order.id === orderId ? { ...order, status: 'cancelled' } : order));
  };

  return <OrdersContext.Provider value={{ orders, placeOrder, cancelOrder }}>{children}</OrdersContext.Provider>;
}

export function useOrders() {
  const context = useContext(OrdersContext);
  if (!context) throw new Error('useOrders must be used inside OrdersProvider');
  return context;
}
