import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { getMenuItemsForRestaurant } from '../../app/customer/menuCatalog.ts';
import { menuItemsMock, menusMock } from '../../mocks/menus.mock.ts';

describe('restaurant menu', () => {
  it('shows only the selected restaurant’s items in menu order', () => {
    const items = getMenuItemsForRestaurant(1, menusMock, menuItemsMock);
    assert.deepEqual(items.map((item) => item.id), [1001, 1002, 1003, 1004, 1005, 1006]);
    assert.equal(items[2].available, false);
  });

  it('supports more than one menu for a restaurant and no menu for an unknown restaurant', () => {
    const menus = [...menusMock, { id: 105, restaurant_id: 1, name: 'Specials' }];
    const items = [...menuItemsMock, { ...menuItemsMock[6], id: 5000, menu_id: 105 }];
    assert.deepEqual(getMenuItemsForRestaurant(1, menus, items).map((item) => item.id), [1001, 1002, 1003, 1004, 1005, 1006, 5000]);
    assert.deepEqual(getMenuItemsForRestaurant(999, menus, items), []);
  });
});
