// Shared restaurant shape used by fixtures and customer-facing features.
export type RestaurantStatus = 'open' | 'paused' | 'closed';

export interface Restaurant {
  id: number;
  name: string;
  status: RestaurantStatus;
  longitude: number | null;
  latitude: number | null;
  /** Absolute cover photo URL from the API; fixtures leave it unset. */
  photoUrl?: string | null;
}
