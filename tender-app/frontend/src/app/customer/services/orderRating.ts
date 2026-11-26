import type { RatingSubmission } from './orderApi';

/** Only delivered orders can be rated (R-RVW-01); the backend refuses a second rating. */
export function canRateOrder(status: string): boolean {
  return status === 'delivered';
}

/** Builds the request body, leaving out courier stars when no courier delivered the order. */
export function toRatingSubmission(restaurantStars: number, courierStars: number | null, comment: string,
  hasCourier: boolean): RatingSubmission {
  const trimmed = comment.trim();
  return {
    restaurantStars,
    ...(hasCourier && courierStars != null ? { courierStars } : {}),
    ...(trimmed ? { comment: trimmed } : {}),
  };
}

/** Why the form cannot be sent yet, or null when it is complete. */
export function ratingFormProblem(restaurantStars: number | null, courierStars: number | null, hasCourier: boolean): string | null {
  if (restaurantStars == null) return 'Choose a rating for the restaurant.';
  if (hasCourier && courierStars == null) return 'Choose a rating for the courier.';
  return null;
}

/** Text for the restaurant panel, e.g. "4.3 (12 ratings)"; null average means no ratings yet. */
export function formatAverageRating(average: number | null | undefined, count: number | null | undefined): string {
  if (average == null || !count) return 'No ratings yet';
  return `${average.toFixed(1)} (${count} ${count === 1 ? 'rating' : 'ratings'})`;
}
