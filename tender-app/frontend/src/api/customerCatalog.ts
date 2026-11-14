import type { MenuItem, MenuItemOption } from '@/mocks/types';
import type { Restaurant, RestaurantStatus } from '@/types/restaurant';
import { API_URL, requestJson } from './client.ts';

/** Menu item as shown to customers; category drives the menu sections. */
export interface CatalogItem extends MenuItem {
  restaurantId: number;
  category: string | null;
}

export interface CatalogMenu {
  items: CatalogItem[];
  options: MenuItemOption[];
}

type RestaurantDto = { id: number; name: string; status: string; latitude: number | null; longitude: number | null; photoUrl: string | null };
type MenuItemDto = { id: number; menu_id: number; name: string; price: number; description: string | null; available: boolean; photoUrl: string | null; category?: string | null };
type MenuItemOptionDto = { id: number; menu_item_id: number; name: string; additional_cost: number };
type MenuResponseDto = { menuItems: MenuItemDto[]; menuItemOptions: MenuItemOptionDto[] };

/** Resolve backend image paths, including URLs stored by older demo seeds. */
export function resolvePhotoUrl(url: string | null): string | null {
  const value = url?.trim();
  if (!value) return null;
  if (value.startsWith('/') && !value.startsWith('//')) return `${API_URL}${value}`;
  try {
    const parsed = new URL(value);
    if (['localhost', '127.0.0.1', '[::1]'].includes(parsed.hostname)
      && /^\/(menu-items-imgs|menu-photos)\//.test(parsed.pathname)) {
      return `${API_URL}${parsed.pathname}${parsed.search}`;
    }
  } catch { /* Keep existing external or data URLs unchanged. */ }
  return value;
}

const STATUSES: RestaurantStatus[] = ['open', 'paused', 'closed'];

export async function fetchOpenRestaurants(token: string): Promise<Restaurant[]> {
  const restaurants = await requestJson<RestaurantDto[]>('GET', '/api/restaurant/open', undefined, token);
  return restaurants.map((restaurant) => ({
    id: restaurant.id,
    name: restaurant.name,
    status: STATUSES.find((status) => status === restaurant.status?.toLowerCase()) ?? 'closed',
    latitude: restaurant.latitude,
    longitude: restaurant.longitude,
    photoUrl: resolvePhotoUrl(restaurant.photoUrl),
  }));
}

export async function fetchRestaurantMenu(token: string, restaurantId: number): Promise<CatalogMenu> {
  const menu = await requestJson<MenuResponseDto>('GET', `/api/restaurant/${restaurantId}/menu`, undefined, token);
  return {
    items: menu.menuItems.map((item) => ({
      id: item.id,
      menu_id: item.menu_id,
      restaurantId,
      name: item.name,
      // BigDecimal arrives as a JSON number, but coerce in case a proxy stringifies it.
      price: Number(item.price),
      description: item.description ?? '',
      available: item.available,
      photURL: resolvePhotoUrl(item.photoUrl),
      category: item.category ?? null,
    })),
    options: menu.menuItemOptions.map((option) => ({
      id: option.id,
      menu_item_id: option.menu_item_id,
      name: option.name,
      additional_cost: Number(option.additional_cost),
    })),
  };
}
