import type { Order, OrderLine } from '../../types/orders';
import type { Restaurant, RestaurantStatus } from '../../types/restaurant';
import type { ProfileResponse, ProfileUpdateRequest } from './operatorApi';
import type { AuthUser } from '../../types/auth';
import type { CustomerProfile, User } from '../../types/users';
import type { OperatorOrder } from '../../types/operator';



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

export function availableStatusChanges(current: RestaurantStatus): RestaurantStatus[] {
  return RESTAURANT_STATUSES.filter((status) => status !== current);
}

export function applyRestaurantStatus(
  restaurants: Restaurant[],
  restaurantId: number,
  status: RestaurantStatus,
): Restaurant[] {
  return restaurants.map((r) => (r.id === restaurantId ? { ...r, status } : r));
}

export type OrderStatus=OperatorOrder['status'];

const ORDER_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  placed: ['accepted', 'rejected'],
  accepted: ['preparing', 'cancelled'],
  preparing: ['ready', 'cancelled'],
  ready: ['picked_up', 'cancelled'],
  picked_up: ['delivered'],
  delivered: ['refunded'], // R-PRC-05: refunded only from delivered, by operator command
  rejected: [],
  cancelled: [],
  refunded: [],
};

export function availableOrderStatusChanges(current:OrderStatus):OrderStatus[]{
  return ORDER_TRANSITIONS[current]??[];
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
  let changedPhone: string | undefined;

  const phone = input.phone.trim();
  if (phone !== '' && phone !== (current?.phone ?? '')) {
    const digits = phone.replace(/[\s\-().]/g, '').replace(/^\+/, '');
    if (/^\d{10,15}$/.test(digits)) changedPhone = phone;
    else errors.phone = 'Enter a valid phone number (10 to 15 digits).';
  }

  if (input.password !== '') {
    errors.password = 'Password changes are not supported yet.';
  }

  if (!current || current.role !== 'operator' || !current.name.trim()) {
    errors.form = 'Please sign in with an operator account again.';
  } else if (Object.keys(errors).length === 0 && !changedPhone) {
    errors.form = 'Change your phone number first.';
  }
  if (Object.keys(errors).length > 0 || !current || !changedPhone) {
    return { ok: false, errors };
  }
  // The backend requires both fields, even when only the phone changed.
  return { ok: true, request: { name: current.name, phone: changedPhone } };
}

/** Preserve the backend role check instead of relabelling other accounts as operators. */
export function profileFromResponse(response: ProfileResponse): User {
  if (response.role !== 'OPERATOR') throw new Error('This is not an operator account');
  return {
    id: response.id,
    name: response.name,
    email: response.email,
    phone: response.phone,
    role: 'operator',
  };
}
