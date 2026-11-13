export type OperatorSectionName = 'restaurants' | 'orders' | 'customers' | 'couriers';
export type RestaurantStatus = 'open' | 'paused' | 'closed';
export type OperatorOrderStatus = 'placed' | 'accepted' | 'preparing' | 'ready' | 'picked_up' | 'delivered' | 'cancelled' | 'rejected' | 'refunded';

export interface OperatorSummary {
  restaurantCount: number;
  orderCount: number;
  customerCount: number;
  courierCount: number;
  availableCourierCount: number;
}

export interface OperatorPage<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export interface OperatorRestaurant {
  id: number;
  name: string;
  status: RestaurantStatus;
  latitude: number | null;
  longitude: number | null;
  address: string | null;
  phone: string | null;
  queueCount: number;
}

export interface OperatorOrderFilters {
  statuses: OperatorOrderStatus[];
  restaurantIds: number[];
  from: string;
  to: string;
}

export interface OperatorCustomer {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  tdCashBalance: number | null;
  latitude: number | null;
  longitude: number | null;
}

export interface OperatorCourier {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  status: string | null;
  latitude: number | null;
  longitude: number | null;
}

export interface OperatorOrder {
  id: number;
  customerId: number | null;
  restaurantId: number | null;
  courierId: number | null;
  courierOnline?: boolean | null; 
  status: OperatorOrderStatus;
  dateOrdered: string | null;
  acceptedAt: string | null;
  rejectionReason: string | null;
  totalPrice: number;
}

export type OperatorSection =
  | { kind: 'restaurants'; data: OperatorPage<OperatorRestaurant> }
  | { kind: 'orders'; data: OperatorPage<OperatorOrder> }
  | { kind: 'customers'; data: OperatorPage<OperatorCustomer> }
  | { kind: 'couriers'; data: OperatorPage<OperatorCourier> };