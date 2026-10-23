import type { CourierProfile, CustomerProfile, RestaurantStaff, User } from './types';

export const usersMock: User[] = [
  { id: 1, role: 'customer', name: 'Alex Morgan', email: 'alex@example.test', password_hash: 'demo-only' },
  { id: 2, role: 'customer', name: 'Sam Chen', email: 'sam@example.test', password_hash: 'demo-only' },
  { id: 3, role: 'restaurant_staff', name: 'Jamie Roy', email: 'jamie@example.test', password_hash: 'demo-only' },
  { id: 4, role: 'courier', name: 'Taylor Singh', email: 'taylor@example.test', password_hash: 'demo-only' },
  { id: 5, role: 'Operator', name: 'Morgan Lee', email: 'morgan@example.test', password_hash: 'demo-only' },
  { id: 6, role: 'restaurant_staff', name: 'Chris Bouchard', email: 'chris@example.test', password_hash: 'demo-only' },
];

export const customerProfilesMock: CustomerProfile[] = [
  { user_id: 1, td_cash_balance: 25, latitude: 45.504, longitude: -73.571 },
  { user_id: 2, td_cash_balance: 8.5, latitude: 45.511, longitude: -73.584 },
];

export const courierProfilesMock: CourierProfile[] = [
  { user_id: 4, latitude: 45.506, longitude: -73.575, status: 'carrying' },
];

export const restaurantStaffMock: RestaurantStaff[] = [
  { user_id: 3, restaurant_id: 1 },
  { user_id: 6, restaurant_id: 2 },
];
