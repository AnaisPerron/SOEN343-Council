import { createContext, useContext, useState, type ReactNode } from 'react';
import { API_URL } from '../api/client';
import type { RestaurantCartGroup } from '../app/customer/cart/cartStore';
import { cancelPlacedOrder, createPlacedOrders, type CustomerOrder } from '../app/customer/services/orderLifecycle';
import { sendOrder, sendOrderCancellation, toOrderSubmission } from '../app/customer/services/orderApi';
import { useAuth } from './AuthContext';

export interface OrderFailure {
  restaurantId: number;
  message: string;
}

interface OrdersContextValue {
  orders: CustomerOrder[];
  /** Places one backend order per restaurant; a failed restaurant does not undo the others. */
  placeOrders: (groups: RestaurantCartGroup[]) => Promise<{ placed: CustomerOrder[]; failures: OrderFailure[] }>;
  /** Rejects with the backend's reason when the order can no longer be cancelled. */
  cancelOrder: (orderId: string) => Promise<void>;
}

const OrdersContext = createContext<OrdersContextValue | null>(null);

const errorMessage = (error: unknown) => error instanceof Error ? error.message : 'Something went wrong.';

export function OrdersProvider({ children }: { children: ReactNode }) {
  const { token } = useAuth();
  // Orders placed in this session; the backend has no customer order list endpoint yet.
  const [orders, setOrders] = useState<CustomerOrder[]>([]);

  const placeOrders: OrdersContextValue['placeOrders'] = async (groups) => {
    const placed: CustomerOrder[] = [];
    const failures: OrderFailure[] = [];
    // Send restaurants one at a time so each charge and failure is attributed to its own order.
    for (const group of groups) {
      try {
        if (!token) throw new Error('Sign in again to place your order.');
        const receipt = await sendOrder(`${API_URL}/api/orders`, token, toOrderSubmission(group));
        const [order] = createPlacedOrders([group], receipt.placedAt, receipt.orderId);
        placed.push({ ...order, total: receipt.total });
      } catch (error) {
        failures.push({ restaurantId: group.restaurantId, message: errorMessage(error) });
      }
    }
    setOrders((current) => [...placed, ...current]);
    return { placed, failures };
  };

  const cancelOrder: OrdersContextValue['cancelOrder'] = async (orderId) => {
    if (!token) throw new Error('Sign in again to cancel your order.');
    await sendOrderCancellation(`${API_URL}/api/orders/${encodeURIComponent(orderId)}/cancel`, token);
    setOrders((current) => current.map((order) => order.id === orderId ? cancelPlacedOrder(order) : order));
  };

  return <OrdersContext.Provider value={{ orders, placeOrders, cancelOrder }}>{children}</OrdersContext.Provider>;
}

export function useOrders() {
  const context = useContext(OrdersContext);
  if (!context) throw new Error('useOrders must be used inside OrdersProvider');
  return context;
}
