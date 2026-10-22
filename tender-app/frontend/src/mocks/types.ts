/** Frontend fixture types based on Documents/Diagrams/ERD.drawio. */
export type UserRole = 'customer' | 'restaurant_staff' | 'courier' | 'admin';
export type MenuItemStatus = 'available' | 'sold_out';
export type CourierStatus = 'offline' | 'idle' | 'assigned' | 'carrying';
export type OrderStatus =
  | 'placed'
  | 'accepted'
  | 'preparing'
  | 'ready'
  | 'picked_up'
  | 'delivered'
  | 'rejected'
  | 'cancelled';

export interface User {
  id: number;
  role: UserRole;
  name: string;
  email: string;
  /** Demo-only placeholder; never use real credentials in fixtures. */
  password_hash: string;
}

export interface RestaurantStaff {
  user_id: number;
  restaurant_id: number;
}

export interface CustomerProfile {
  user_id: number;
  td_cash_balance: number;
  latitude: number | null;
  longitude: number | null;
}

export interface CourierProfile {
  user_id: number;
  latitude: number | null;
  longitude: number | null;
  status: CourierStatus;
}

export interface Menu {
  id: number;
  restaurant_id: number;
  name: string;
}

export interface MenuItem {
  id: number;
  menu_id: number;
  name: string;
  price: number;
  status: MenuItemStatus;
  photo_url: string | null;
}

export interface MenuItemOption {
  id: number;
  menu_item_id: number;
  name: string;
  additional_cost: number;
}

export interface Order {
  id: number;
  customer_id: number;
  restaurant_id: number;
  courier_id: number | null;
  status: OrderStatus;
  created_at: string;
  accepted_at: string | null;
  rejection_reason: string | null;
}

export interface OrderLine {
  id: number;
  order_id: number;
  menu_item_id: number;
  quantity: number;
  unit_price: number;
}

export interface OrderEvent {
  id: number;
  order_id: number;
  previous_status: OrderStatus | null;
  new_status: OrderStatus;
  event_type: string;
  created_at: string;
}
