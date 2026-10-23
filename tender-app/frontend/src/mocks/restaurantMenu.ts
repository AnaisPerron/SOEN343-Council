import type { RestaurantMenu } from '../api/restaurantMenu';

const STORAGE_KEY = 'tender-demo-little-italy-menu';
let memoryMenu: RestaurantMenu | null = null;

export const emptyDemoRestaurantMenu = (): RestaurantMenu => ({
  restaurantId: 4,
  name: 'Little Italy Pizza',
  status: 'closed',
  address: '',
  deliveryFee: 0,
  coverUrl: null,
  menuName: '',
  items: [],
});

export function loadDemoRestaurantMenu(): RestaurantMenu | null {
  if (typeof localStorage !== 'undefined') {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? JSON.parse(saved) as RestaurantMenu : null;
    } catch {
      return null;
    }
  }
  return memoryMenu;
}

export function saveDemoRestaurantMenu(menu: RestaurantMenu): RestaurantMenu {
  const saved = JSON.parse(JSON.stringify(menu)) as RestaurantMenu;
  if (typeof localStorage !== 'undefined') localStorage.setItem(STORAGE_KEY, JSON.stringify(saved));
  memoryMenu = saved;
  return saved;
}
