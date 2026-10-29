import { API_URL, ApiError } from './client.ts';
import { isDemoRestaurantToken } from '../mocks/restaurantAccount.ts';
import { loadDemoRestaurantMenu, saveDemoRestaurantMenu } from '../mocks/restaurantMenu.ts';

export type MenuOption = { id: number | null; name: string; additionalCost: number };
export type MenuItem = { id: number | null; name: string; description: string; category: string; price: number; status: 'available' | 'sold_out'; photoUrl: string | null; options: MenuOption[] };
export type RestaurantMenu = { restaurantId: number; name: string; status: 'open' | 'paused' | 'closed'; address: string; deliveryFee: number; coverUrl: string | null; menuName: string; items: MenuItem[] };

async function request(method: 'GET' | 'PUT', token: string, body?: RestaurantMenu): Promise<RestaurantMenu> {
  if (isDemoRestaurantToken(token)) {
    if (method === 'PUT' && body) return saveDemoRestaurantMenu(token, body);
    const menu = loadDemoRestaurantMenu(token);
    if (!menu) throw new ApiError(404, 'No menu has been created yet');
    return menu;
  }
  let response: Response;
  try {
    response = await fetch(`${API_URL}/api/restaurant/menu`, {
      method,
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      ...(body ? { body: JSON.stringify(body) } : {}),
    });
  } catch {
    throw new ApiError(0, `Cannot reach the server at ${API_URL}`);
  }
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new ApiError(response.status, data?.message ?? `Request failed (${response.status})`);
  return data as RestaurantMenu;
}

export const loadRestaurantMenu = (token: string) => request('GET', token);
export const saveRestaurantMenu = (token: string, menu: RestaurantMenu) => request('PUT', token, menu);
