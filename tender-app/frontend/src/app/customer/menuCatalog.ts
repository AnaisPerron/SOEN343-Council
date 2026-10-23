import type { Menu, MenuItem } from '../../mocks/types';

export function getMenuItemsForRestaurant(
  restaurantId: number,
  menus: Menu[],
  items: MenuItem[],
): MenuItem[] {
  const menuIds = new Set(menus.filter((menu) => menu.restaurant_id === restaurantId).map((menu) => menu.id));
  return items.filter((item) => menuIds.has(item.menu_id));
}
