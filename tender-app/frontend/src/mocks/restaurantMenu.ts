import type { RestaurantMenu } from '../api/restaurantMenu';
import { demoRestaurantForToken } from './restaurantAccount.ts';

const STORAGE_PREFIX = 'tender-demo-restaurant-menu-v2:';
const memoryMenus = new Map<number, RestaurantMenu>();

export const emptyRestaurantMenu = (restaurantId: number, name: string): RestaurantMenu => ({
  restaurantId,
  name,
  status: 'closed',
  address: '',
  deliveryFee: 0,
  coverUrl: null,
  menuName: '',
  items: [],
});

const greenBowlMenu = (): RestaurantMenu => ({
  ...emptyRestaurantMenu(2, 'Green Bowl Kitchen'),
  status: 'open',
  menuName: 'Green Bowl Menu',
  items: [
    { id: 2001, name: 'Harvest grain bowl', description: 'Seasonal vegetables, grains and house dressing.', category: 'Bowls', price: 15.75, status: 'available', photoUrl: null, options: [{ id: 5003, name: 'Add grilled chicken', additionalCost: 4 }] },
    { id: 2002, name: 'Miso tofu bowl', description: 'Miso tofu with fresh vegetables and rice.', category: 'Bowls', price: 14, status: 'available', photoUrl: null, options: [{ id: 5007, name: 'Extra tofu', additionalCost: 3 }] },
    { id: 2003, name: 'Truffle fries', description: 'Golden fries tossed with fragrant truffle.', category: 'Sides', price: 6.25, status: 'available', photoUrl: null, options: [] },
  ],
});

export function loadDemoRestaurantMenu(token: string): RestaurantMenu | null {
  const account = demoRestaurantForToken(token);
  if (!account) return null;
  const savedInMemory = memoryMenus.get(account.restaurantId);
  if (savedInMemory) return savedInMemory;
  if (typeof localStorage !== 'undefined') {
    try {
      const saved = localStorage.getItem(`${STORAGE_PREFIX}${account.restaurantId}`);
      if (saved) return JSON.parse(saved) as RestaurantMenu;
    } catch { /* Use the initial menu if browser storage is unavailable. */ }
  }
  return account.restaurantId === 2 ? greenBowlMenu() : null;
}

export function saveDemoRestaurantMenu(token: string, menu: RestaurantMenu): RestaurantMenu {
  const account = demoRestaurantForToken(token);
  if (!account || menu.restaurantId !== account.restaurantId) throw new Error('This menu does not belong to this demo restaurant.');
  const saved = JSON.parse(JSON.stringify(menu)) as RestaurantMenu;
  if (typeof localStorage !== 'undefined') localStorage.setItem(`${STORAGE_PREFIX}${account.restaurantId}`, JSON.stringify(saved));
  memoryMenus.set(account.restaurantId, saved);
  return saved;
}
