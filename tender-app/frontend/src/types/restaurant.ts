// Shared restaurant shape used by fixtures and customer-facing features.
export type RestaurantStatus = 'open' | 'paused' | 'closed';

export interface Restaurant {
  id: number;
  name: string;
  status: RestaurantStatus;
  longitude: number | null;
  latitude: number | null;
}
