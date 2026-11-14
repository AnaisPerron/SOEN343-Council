import { resolvePhotoUrl } from '../../api/customerCatalog.ts';
import { API_URL } from '../../api/client.ts';
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { getMenuItemsForRestaurant } from '../../app/customer/services/menuCatalog.ts';
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

describe('catalog photo URLs', () => {
  it('resolves seeded covers and menu photos against the configured backend', () => {
    assert.equal(resolvePhotoUrl('/menu-items-imgs/truffle.jpg'), `${API_URL}/menu-items-imgs/truffle.jpg`);
    assert.equal(resolvePhotoUrl('/menu-photos/4'), `${API_URL}/menu-photos/4`);
  });

  it('rebases older localhost image URLs for remote browsers and devices', () => {
    assert.equal(resolvePhotoUrl('http://localhost:8080/menu-items-imgs/truffle.jpg'), `${API_URL}/menu-items-imgs/truffle.jpg`);
    assert.equal(resolvePhotoUrl('http://127.0.0.1:8080/menu-photos/4'), `${API_URL}/menu-photos/4`);
  });

  it('preserves external and data image URLs', () => {
    for (const url of ['https://cdn.example.test/pizza.jpg', 'data:image/jpeg;base64,/9j/', '//cdn.example.test/pizza.jpg']) {
      assert.equal(resolvePhotoUrl(url), url);
    }
  });

  it('handles empty photo values', () => {
    assert.equal(resolvePhotoUrl(null), null);
    assert.equal(resolvePhotoUrl(''), null);
    assert.equal(resolvePhotoUrl('   '), null);
  });
});
