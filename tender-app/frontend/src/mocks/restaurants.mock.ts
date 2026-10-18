import type { Restaurant } from './types';

export const restaurantsMock: Restaurant[] = [
  { id: 1, name: 'Maple & Main', status: 'open', latitude: 45.5019, longitude: -73.5674 },
  { id: 2, name: 'Green Bowl Kitchen', status: 'open', latitude: 45.5088, longitude: -73.5878 },
  { id: 3, name: 'Café Nord', status: 'paused', latitude: 45.4972, longitude: -73.579 },
  { id: 4, name: 'Little Italy Pizza', status: 'closed', latitude: 45.5352, longitude: -73.6132 },
];
