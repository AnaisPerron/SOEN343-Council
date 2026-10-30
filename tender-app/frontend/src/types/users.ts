export type UserRole = 'customer' | 'courier' | 'operator' | 'restaurant_staff';

export interface User {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  password_hash?: string;
  phone?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  created_at?: string;
}

export interface CustomerProfile {
  user_id: number;
  td_cash_balance?: number;
  latitude?: number;
  longitude?: number;
}

export interface CourierProfile {
  user_id: number;
  latitude?: number;
  longitude?: number;
  status?: 'idle' | 'carrying' | 'offline';
}

export interface RestaurantStaff {
  user_id: number;
  restaurant_id: number;
}