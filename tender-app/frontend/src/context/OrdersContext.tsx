import { createContext, useContext, useRef, useState, type ReactNode } from 'react';
import type { RestaurantCartGroup } from '../app/customer/cartStore';
import { cancelPlacedOrder, createPlacedOrders, type CustomerOrder } from '../app/customer/orderLifecycle';

interface OrdersContextValue {
  orders: CustomerOrder[];
  placeOrders: (groups: RestaurantCartGroup[]) => CustomerOrder[];
  cancelOrder: (orderId: string) => void;
}

const OrdersContext = createContext<OrdersContextValue | null>(null);

export function OrdersProvider({ children }: { children: ReactNode }) {
  const [orders, setOrders] = useState<CustomerOrder[]>([]);
  const nextId = useRef(1);

  const placeOrders = (groups: RestaurantCartGroup[]) => {
    const placed = createPlacedOrders(groups, new Date().toISOString(), nextId.current);
    nextId.current += placed.length;
    setOrders((current) => [...placed, ...current]);
    return placed;
  };

  const cancelOrder = (orderId: string) => {
    // TODO: Send cancellation to the backend before updating the local order status.
    setOrders((current) => current.map((order) => order.id === orderId ? cancelPlacedOrder(order) : order));
  };

  return <OrdersContext.Provider value={{ orders, placeOrders, cancelOrder }}>{children}</OrdersContext.Provider>;
}

export function useOrders() {
  const context = useContext(OrdersContext);
  if (!context) throw new Error('useOrders must be used inside OrdersProvider');
  return context;
}
