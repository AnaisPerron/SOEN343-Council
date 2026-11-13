import type { Restaurant } from '@/types/restaurant';
import type { MenuItem } from '@/mocks/types';

export const ORDER_RESTRICTION_CODE = 'R-ORD-02 - ORD';
export const ORDER_ERROR_FADE_DELAY_MS = 1500;
export const ORDER_ERROR_FADE_DURATION_MS = 500;

export interface RestaurantOrderRestriction {
  code: typeof ORDER_RESTRICTION_CODE;
  message: string;
}

export interface PlaceOrderAvailability {
  disabled: boolean;
  hint: string | undefined;
}

export function getPlaceOrderAvailability(
  restriction: RestaurantOrderRestriction | null,
  selectedItemAvailability: readonly (boolean | undefined)[],
  submitting = false,
): PlaceOrderAvailability {
  const hasItems = selectedItemAvailability.length > 0;
  const everySelectedItemSoldOut = hasItems && selectedItemAvailability.every((available) => available === false);
  const hint = restriction?.message
    ?? (everySelectedItemSoldOut ? 'All items in your cart are sold out.' : !hasItems ? 'Add a menu item before placing an order.' : undefined);
  return {
    disabled: submitting || !hasItems || restriction !== null || everySelectedItemSoldOut,
    hint,
  };
}

// R-ORD-02 - ORD allows order placement only while a restaurant is open.
export function getRestaurantOrderRestriction(
  restaurant: Restaurant,
): RestaurantOrderRestriction | null {
  if (restaurant.status === 'open') return null;

  return {
    code: ORDER_RESTRICTION_CODE,
    message: `Orders are unavailable at ${restaurant.name} because it is ${restaurant.status}.`,
  };
}

export function getCartOrderRestriction(
  restaurantIds: number[],
  restaurants: Restaurant[],
): RestaurantOrderRestriction | null {
  for (const id of restaurantIds) {
    const restaurant = restaurants.find((entry) => entry.id === id);
    if (!restaurant) {
      return {
        code: ORDER_RESTRICTION_CODE,
        message: 'Orders are unavailable because a restaurant in your cart could not be found.',
      };
    }
    const restriction = getRestaurantOrderRestriction(restaurant);
    if (restriction) return restriction;
  }
  return null;
}

export function getMenuItemOrderRestriction(item: MenuItem): RestaurantOrderRestriction | null {
  if (item.available) return null;

  return {
    code: ORDER_RESTRICTION_CODE,
    message: `${item.name} is not available right now and cannot be added to your cart.`,
  };
}
