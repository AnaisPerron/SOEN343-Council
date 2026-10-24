const PREFIX = 'tender-restaurant-cover:';
const sessionImages = new Map<number, string | null>();

export function readRestaurantCover(accountId: number): string | null | undefined {
  if (sessionImages.has(accountId)) return sessionImages.get(accountId);
  if (typeof localStorage === 'undefined') return undefined;
  try {
    const value = localStorage.getItem(`${PREFIX}${accountId}`);
    if (value === null) return undefined;
    return value === '' ? null : value;
  } catch {
    return undefined;
  }
}

export function writeRestaurantCover(accountId: number, image: string | null): void {
  sessionImages.set(accountId, image);
  if (typeof localStorage !== 'undefined') localStorage.setItem(`${PREFIX}${accountId}`, image ?? '');
}
