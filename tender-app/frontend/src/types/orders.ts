export type OrderStatus =
  | 'placed'
  | 'accepted'
  | 'preparing'
  | 'ready'
  | 'picked_up'
  | 'delivered'
  | 'rejected'
  | 'cancelled';

export type OrderEventType =
  | 'order_placed'
  | 'restaurant_accepted'
  | 'preparation_started'
  | 'restaurant_rejected'
  | 'customer_cancelled'
  | 'order_ready'
  | 'courier_picked_up'
  | 'order_delivered';

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
  event_type: OrderEventType;
  created_at: string;
}