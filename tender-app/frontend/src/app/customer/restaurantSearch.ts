import type { Restaurant } from '../../types/restaurant';

/** Search restaurant names with whitespace trimming and locale-aware case folding. */
export function searchRestaurants(restaurants: Restaurant[], query: string): Restaurant[] {
  const normalizedQuery = query.trim().toLocaleLowerCase();
  if (!normalizedQuery) return [];

  return restaurants.filter((restaurant) =>
    restaurant.name.toLocaleLowerCase().includes(normalizedQuery),
  );
}
