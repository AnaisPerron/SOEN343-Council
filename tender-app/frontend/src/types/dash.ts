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
}

export interface MapCourier {
  courierId: number;
  name: string;
  status: Exclude<CourierState, 'offline'>;
  latitude: number;
  longitude: number;
}

/** When nothing on the map has a location, bounds is a default area and both lists are empty. */
export interface MapData {
  bounds: MapBounds;
  restaurants: MapRestaurant[];
  couriers: MapCourier[];
}
