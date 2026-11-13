import type { RestaurantStatus } from '../types/restaurant.ts';

export type QueueBand = 'short' | 'building' | 'long';

/** R-DASH-03: paused restaurants are always long; otherwise band by queued preparation orders. */
export function queueBand(queueCount: number, status: RestaurantStatus | string): QueueBand {
  if (status === 'paused' || queueCount >= 6) return 'long';
  if (queueCount >= 3) return 'building';
  return 'short';
}

export const queueBandColor: Record<QueueBand, string> = {
  short: '#16a34a',
  building: '#f59e0b',
  long: '#dc2626',
};
