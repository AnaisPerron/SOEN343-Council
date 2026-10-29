const PREFIX = 'tender-restaurant-item-photo:';
const sessionImages = new Map<string, string | null>();

const key = (accountId: number, itemId: number) => `${PREFIX}${accountId}:${itemId}`;

export function readRestaurantItemPhoto(accountId: number, itemId: number): string | null | undefined {
  const storageKey = key(accountId, itemId);
  if (sessionImages.has(storageKey)) return sessionImages.get(storageKey);
  if (typeof localStorage === 'undefined') return undefined;
  try {
    const value = localStorage.getItem(storageKey);
    if (value === null) return undefined;
    return value === '' ? null : value;
  } catch {
    return undefined;
  }
}

export function writeRestaurantItemPhoto(accountId: number, itemId: number, image: string | null): void {
  const storageKey = key(accountId, itemId);
  sessionImages.set(storageKey, image);
  if (typeof localStorage !== 'undefined') localStorage.setItem(storageKey, image ?? '');
}

export function clearRestaurantItemPhoto(accountId: number, itemId: number): void {
  const storageKey = key(accountId, itemId);
  sessionImages.delete(storageKey);
  if (typeof localStorage !== 'undefined') localStorage.removeItem(storageKey);
}
