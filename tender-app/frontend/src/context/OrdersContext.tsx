import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { API_URL } from '../api/client';
import type { RestaurantCartGroup } from '../app/customer/cart/cartStore';
import { cancelPlacedOrder, createPlacedOrders, fromOrderHistory, type CustomerOrder } from '../app/customer/services/orderLifecycle';
import { fetchOrderHistory, sendOrder, sendOrderCancellation, toOrderSubmission, type OrderSubmissionResult } from '../app/customer/services/orderApi';
import { useAuth } from './AuthContext';

export interface OrderFailure {
  restaurantId: number;
  message: string;
}

interface OrdersContextValue {
  orders: CustomerOrder[];
  historyStatus: 'loading' | 'ready' | 'error';
  historyError: string | null;
  /** Reload the customer's orders from the backend, e.g. to pick up status changes. */
  refreshOrders: () => void;
  /** Places one backend order per restaurant; a failed restaurant does not undo the others. */
  placeOrders: (groups: RestaurantCartGroup[], destination?: { address: string; latitude: number; longitude: number }, tenderAllocations?: Record<number, number>) => Promise<{ placed: CustomerOrder[]; failures: OrderFailure[] }>;
  recordPaidOrders: (groups: RestaurantCartGroup[], receipts: OrderSubmissionResult[]) => CustomerOrder[];
  /** Rejects with the backend's reason when the order can no longer be cancelled. */
  cancelOrder: (orderId: string) => Promise<void>;
}

const OrdersContext = createContext<OrdersContextValue | null>(null);

const errorMessage = (error: unknown) => error instanceof Error ? error.message : 'Something went wrong.';

export function OrdersProvider({ children }: { children: ReactNode }) {
  const { token, user } = useAuth();
  const [orders, setOrders] = useState<CustomerOrder[]>([]);
  const [historyStatus, setHistoryStatus] = useState<OrdersContextValue['historyStatus']>('loading');
  const [historyError, setHistoryError] = useState<string | null>(null);
  const isCustomer = user?.role === 'customer';

  // Only update state once the request settles, so this can run directly from an effect.
  const loadHistory = useCallback(() => {
    if (!token || !isCustomer) return;
    fetchOrderHistory(`${API_URL}/api/orders`, token)
      .then((entries) => { setOrders(entries.map(fromOrderHistory)); setHistoryStatus('ready'); setHistoryError(null); })
      .catch((error) => { setHistoryError(errorMessage(error)); setHistoryStatus('error'); });
  }, [token, isCustomer]);

  useEffect(loadHistory, [loadHistory]);

  const placeOrders: OrdersContextValue['placeOrders'] = async (groups, destination, tenderAllocations = {}) => {
    const placed: CustomerOrder[] = [];
    const failures: OrderFailure[] = [];
    // Send restaurants one at a time so each charge and failure is attributed to its own order.
    for (const group of groups) {
      try {
        if (!token) throw new Error('Sign in again to place your order.');
        const receipt = await sendOrder(`${API_URL}/api/orders`, token,
          { ...toOrderSubmission(group, destination), tendersToUse: tenderAllocations[group.restaurantId] ?? 0 });
        const [order] = createPlacedOrders([group], receipt.placedAt, receipt.orderId);
        placed.push({ ...order, total: receipt.total });
      } catch (error) {
        failures.push({ restaurantId: group.restaurantId, message: errorMessage(error) });
      }
    }
    setOrders((current) => [...placed, ...current]);
    // Swap the local copies for the server's versions, which carry restaurant names and add-on details.
    if (placed.length > 0) loadHistory();
    return { placed, failures };
  };

  const recordPaidOrders: OrdersContextValue['recordPaidOrders'] = (groups, receipts) => {
    const placed = receipts.map((receipt) => {
      const group = groups.find((entry) => entry.restaurantId === receipt.restaurantId);
      if (!group) throw new Error('PayPal returned an unexpected restaurant order.');
      const [order] = createPlacedOrders([group], receipt.placedAt, receipt.orderId);
      return { ...order, total: Number(receipt.total) };
    });
    setOrders((current) => [...placed, ...current]);
    loadHistory();
    return placed;
  };

  const cancelOrder: OrdersContextValue['cancelOrder'] = async (orderId) => {
    if (!token) throw new Error('Sign in again to cancel your order.');
    await sendOrderCancellation(`${API_URL}/api/orders/${encodeURIComponent(orderId)}/cancel`, token);
    setOrders((current) => current.map((order) => order.id === orderId ? cancelPlacedOrder(order) : order));
  };

  return <OrdersContext.Provider value={{ orders, historyStatus, historyError, refreshOrders: loadHistory, placeOrders, recordPaidOrders, cancelOrder }}>{children}</OrdersContext.Provider>;
}

export function useOrders() {
  const context = useContext(OrdersContext);
  if (!context) throw new Error('useOrders must be used inside OrdersProvider');
  return context;
}
