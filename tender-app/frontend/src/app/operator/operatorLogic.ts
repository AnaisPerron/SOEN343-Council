import type { Order, OrderLine } from '../../types/orders';
import type { Restaurant, RestaurantStatus } from '../../types/restaurant';
import type { ProfileResponse, ProfileUpdateRequest } from './operatorTypes';
import type { AuthUser } from '../../types/auth';
import type { CustomerProfile, User } from '../../types/users';

export const ACTIVE_ORDER_STATUSES: Order['status'][] = [
  'placed',
  'accepted',
  'preparing',
  'ready',
  'picked_up',
];

export function countActiveOrders(orders: Order[]): number {
  return orders.filter((o) => ACTIVE_ORDER_STATUSES.includes(o.status)).length;
}

export function countOpenRestaurants(restaurants: Restaurant[]): number {
  return restaurants.filter((r) => r.status === 'open').length;
}

export function calculateOrderTotal(orderId: number, lines: OrderLine[]): string {
  const total = lines
    .filter((line) => line.order_id === orderId)
    .reduce((sum, line) => sum + line.quantity * line.unit_price, 0);
  return total.toFixed(2);
}

export function getCustomerBalance(userId: number, profiles: CustomerProfile[]): string {
  const balance = profiles.find((p) => p.user_id === userId)?.td_cash_balance;
  return balance != null ? balance.toFixed(2) : '0.00';
}

export function formatRegisteredSummary(count: number, singular: string): string {
  return `${count} Registered ${count === 1 ? singular : `${singular}s`}`;
}


export function toOperatorUser(authUser: AuthUser | null | undefined): User | undefined {
  if (!authUser || authUser.role !== 'Operator') return undefined;
  return {
    id: authUser.id,
    name: authUser.name,
    email: authUser.email,
    phone: authUser.phoneNumber,
    role: 'operator',
  };
}

export function getOperatorDisplayName(operator: User | undefined): string {
  return operator?.name?.trim() || 'Operator';
}


export const RESTAURANT_STATUSES: RestaurantStatus[] = ['open', 'paused', 'closed'];

const NOT_CANCELLABLE: Order['status'][] = ['delivered', 'cancelled', 'rejected'];

export function canOperatorCancel(status: Order['status']): boolean {
  return !NOT_CANCELLABLE.includes(status);
}

export function availableStatusChanges(current: RestaurantStatus): RestaurantStatus[] {
  return RESTAURANT_STATUSES.filter((status) => status !== current);
}

export function markOrderCancelled(orders: Order[], orderId: number): Order[] {
  return orders.map((o) => (o.id === orderId ? { ...o, status: 'cancelled' } : o));
}

export function applyRestaurantStatus(
  restaurants: Restaurant[],
  restaurantId: number,
  status: RestaurantStatus,
): Restaurant[] {
  return restaurants.map((r) => (r.id === restaurantId ? { ...r, status } : r));
}


export type ProfileErrors = Partial<Record<'phone' | 'password' | 'form', string>>;
export type ProfileValidation =
  | { ok: true; request: ProfileUpdateRequest }
  | { ok: false; errors: ProfileErrors };


export function validateProfileUpdate(
  input: { phone: string; password: string },
  current?: User,
): ProfileValidation {
  const errors: ProfileErrors = {};
  const request: ProfileUpdateRequest = {};

  const phone = input.phone.trim();
  if (phone !== '' && phone !== (current?.phone ?? '')) {
    const digits = phone.replace(/[\s\-().]/g, '').replace(/^\+/, '');
    if (/^\d{10,15}$/.test(digits)) request.phone = phone;
    else errors.phone = 'Enter a valid phone number (10 to 15 digits).';
  }

  if (input.password !== '') {
    if (input.password.length >= 8 && input.password.length <= 72) request.password = input.password;
    else errors.password = 'Password must be 8 to 72 characters.';
  }

  if (Object.keys(errors).length === 0 && Object.keys(request).length === 0) {
    errors.form = 'Change your phone number or password first.';
  }
  return Object.keys(errors).length > 0 ? { ok: false, errors } : { ok: true, request };
}

/** backend inegratiobn*/
export function profileFromResponse(response: ProfileResponse): User {
  return {
    id: response.id,
    name: response.name,
    email: response.email,
    phone: response.phone,
    role: 'operator',
  };
}