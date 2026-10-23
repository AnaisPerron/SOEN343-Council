import type { MenuItem } from '@/mocks/types';

export interface CartLine {
  // A line represents one item with one specific set of extras.
  key: string;
  itemId: number;
  restaurantId: number;
  optionIds: number[];
  quantity: number;
  // Base item price plus selected extras, before multiplying by quantity.
  unitPrice: number;
}

export interface RestaurantCartGroup {
  restaurantId: number;
  lines: CartLine[];
  subtotal: number;
  deliveryFee: number;
  serviceFee: number;
  total: number;
}

// TODO: Replace these zero-valued fees when delivery and service pricing are supported.
export const DELIVERY_FEE = 0;
export const SERVICE_FEE = 0;

export function addCartLine(lines: CartLine[], item: MenuItem, restaurantId: number, optionIds: number[], optionCost: number, quantity: number): CartLine[] {
  if (!item.available || quantity < 1) return lines;
  // Normalize option order so the same selection always resolves to the same cart line.
  const sortedOptionIds = [...optionIds].sort((a, b) => a - b);
  const key = `${item.id}:${sortedOptionIds.join(',')}`;
  const existing = lines.find((line) => line.key === key);
  // Repeated additions with the same extras increase quantity instead of creating duplicate rows.
  if (existing) return lines.map((line) => line.key === key ? { ...line, quantity: line.quantity + quantity } : line);
  // Capture the chosen unit price when adding; changing cart quantity does not recalculate it.
  return [...lines, { key, itemId: item.id, restaurantId, optionIds: sortedOptionIds, quantity, unitPrice: item.price + optionCost }];
}

export function changeCartLineQuantity(lines: CartLine[], key: string, quantity: number): CartLine[] {
  // Decreasing the last unit removes the line from the draft cart.
  return quantity <= 0 ? lines.filter((line) => line.key !== key) : lines.map((line) => line.key === key ? { ...line, quantity } : line);
}

export function cartTotal(lines: CartLine[]): number {
  return lines.reduce((total, line) => total + line.unitPrice * line.quantity, 0);
}

export function groupCartLinesByRestaurant(lines: CartLine[]): RestaurantCartGroup[] {
  // Preserve the order restaurants were first added to the cart.
  const grouped = new Map<number, CartLine[]>();
  for (const line of lines) {
    const restaurantLines = grouped.get(line.restaurantId) ?? [];
    restaurantLines.push(line);
    grouped.set(line.restaurantId, restaurantLines);
  }

  return [...grouped].map(([restaurantId, restaurantLines]) => {
    const subtotal = cartTotal(restaurantLines);
    // Fees belong to each restaurant's bill, even while their values are placeholders.
    return { restaurantId, lines: restaurantLines, subtotal, deliveryFee: DELIVERY_FEE, serviceFee: SERVICE_FEE, total: subtotal + DELIVERY_FEE + SERVICE_FEE };
  });
}
