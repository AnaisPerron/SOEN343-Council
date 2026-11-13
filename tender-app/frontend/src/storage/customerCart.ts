import type { CartLine } from '../app/customer/cart/cartStore';

const PREFIX = 'tender-customer-cart-v1:';

function isCartLine(value: unknown): value is CartLine {
  if (typeof value !== 'object' || value === null) return false;
  const line = value as Record<string, unknown>;
  return typeof line.key === 'string'
    && typeof line.itemId === 'number'
    && typeof line.restaurantId === 'number'
    && Array.isArray(line.optionIds) && line.optionIds.every((id) => typeof id === 'number')
    && typeof line.quantity === 'number' && line.quantity > 0
    && typeof line.unitPrice === 'number';
}

// Restore the customer's draft cart after a page reload. Native builds have no localStorage, so the cart stays in memory there.
export function loadCustomerCart(userId: number | undefined): CartLine[] {
  if (userId === undefined || typeof localStorage === 'undefined') return [];
  try {
    const saved = localStorage.getItem(`${PREFIX}${userId}`);
    if (!saved) return [];
    const parsed: unknown = JSON.parse(saved);
    // Ignore corrupt or outdated data rather than crashing the cart screen.
    return Array.isArray(parsed) ? parsed.filter(isCartLine) : [];
  } catch {
    return [];
  }
}

export function saveCustomerCart(userId: number | undefined, lines: CartLine[]): void {
  if (userId === undefined || typeof localStorage === 'undefined') return;
  try {
    // Remove the key for an empty cart so cleared items cannot come back.
    if (lines.length === 0) localStorage.removeItem(`${PREFIX}${userId}`);
    else localStorage.setItem(`${PREFIX}${userId}`, JSON.stringify(lines));
  } catch { /* Storage full or blocked: the cart still works for this session. */ }
}
