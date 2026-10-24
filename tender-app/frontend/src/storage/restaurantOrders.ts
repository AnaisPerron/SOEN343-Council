import type { RestaurantOrder } from '../services/restaurant/orderQueue';
import { restaurantOrdersMock } from '../mocks/restaurantOrders';

const PREFIX = 'tender-restaurant-orders-v1:';
const sessionOrders = new Map<number, RestaurantOrder[]>();

export function loadRestaurantOrders(restaurantId: number): RestaurantOrder[] {
  const inSession = sessionOrders.get(restaurantId);
  if (inSession) return inSession;
  if (typeof localStorage !== 'undefined') {
    try {
      const saved = localStorage.getItem(`${PREFIX}${restaurantId}`);
      if (saved) return JSON.parse(saved) as RestaurantOrder[];
    } catch { /* Fall through to fixture data. */ }
  }
  return restaurantOrdersMock.filter((order) => order.restaurantId === restaurantId);
}

export function saveRestaurantOrders(restaurantId: number, orders: RestaurantOrder[]): void {
  sessionOrders.set(restaurantId, orders);
  if (typeof localStorage !== 'undefined') localStorage.setItem(`${PREFIX}${restaurantId}`, JSON.stringify(orders));
}
