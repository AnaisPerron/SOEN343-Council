const PREFIX = 'tender-restaurant-item-photo:';
const sessionImages = new Map<string, string | null>();
const keyFor = (accountId: number, itemId: number) => `${PREFIX}${accountId}:${itemId}`;

export function readRestaurantItemPhoto(accountId: number, itemId: number): string | null | undefined {
  const key = keyFor(accountId, itemId);
  if (sessionImages.has(key)) return sessionImages.get(key);
  if (typeof localStorage === 'undefined') return undefined;
  try {
    const value = localStorage.getItem(key);
    return value === null ? undefined : value === '' ? null : value;
  } catch {
    return undefined;
  }
}

export function writeRestaurantItemPhoto(accountId: number, itemId: number, image: string | null): void {
  const key = keyFor(accountId, itemId);
  sessionImages.set(key, image);
  if (typeof localStorage !== 'undefined') localStorage.setItem(key, image ?? '');
}

export function clearRestaurantItemPhoto(accountId: number, itemId: number): void {
  const key = keyFor(accountId, itemId);
  sessionImages.delete(key);
  if (typeof localStorage !== 'undefined') localStorage.removeItem(key);
}
