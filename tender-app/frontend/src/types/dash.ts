// Shapes returned by GET /api/map for the shared city map.
import type { CourierState } from '../api/courierDuty.ts';
import type { RestaurantStatus } from './restaurant.ts';

export interface MapBounds {
  minLatitude: number;
  minLongitude: number;
  maxLatitude: number;
  maxLongitude: number;
}

export interface MapRestaurant {
  id: number;
  name: string;
  status: RestaurantStatus;
  latitude: number;
  longitude: number;
  queueCount: number;
}

export interface MapCourier {
  courierId: number;
  name: string;
  status: Exclude<CourierState, 'offline'>;
  latitude: number;
  longitude: number;
}

/** A customer's delivery location; only shown to that customer and to the courier bringing their order. */
export interface MapCustomer {
  customerId: number;
  name: string;
  latitude: number;
  longitude: number;
}

/** What the logged-in role may see; when nothing has a location, bounds is a default area and every list is empty. */
export interface MapData {
  bounds: MapBounds;
  restaurants: MapRestaurant[];
  couriers: MapCourier[];
  customers: MapCustomer[];
}
