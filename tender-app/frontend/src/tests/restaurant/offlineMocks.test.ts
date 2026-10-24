import assert from 'node:assert/strict';
import { afterEach, describe, it, mock } from 'node:test';

import { login, logout } from '../../api/auth.ts';
import { ApiError } from '../../api/client.ts';
import { loadRestaurantMenu, saveRestaurantMenu } from '../../api/restaurantMenu.ts';
import { emptyRestaurantMenu } from '../../mocks/restaurantMenu.ts';
import { filterAndSortOrders, transitionOrder, deleteSelectedOrders } from '../../services/restaurant/orderQueue.ts';
import { loadRestaurantOrders, saveRestaurantOrders } from '../../storage/restaurantOrders.ts';

describe('offline restaurant mocks', () => {
  afterEach(() => mock.restoreAll());

  it('keeps both demo accounts, menus, and queues separate without backend or database access', async () => {
    const previousStorage = Object.getOwnPropertyDescriptor(globalThis, 'localStorage');
    const values = new Map<string, string>();
    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      value: {
        getItem: (key: string) => values.get(key) ?? null,
        setItem: (key: string, value: string) => { values.set(key, value); },
      },
    });
    const fetchMock = mock.method(globalThis, 'fetch', async () => { throw new TypeError('Backend offline'); });

    try {
      const green = await login({ role: 'restaurant', email: 'greenbowl@demo.test', password: 'DemoPass123!' });
      const little = await login({ role: 'restaurant', email: 'littleitaly@demo.test', password: 'DemoPass123!' });

      const greenMenu = await loadRestaurantMenu(green.token);
      assert.equal(greenMenu.name, 'Green Bowl Kitchen');
      assert.equal(greenMenu.items.length, 3);
      await assert.rejects(loadRestaurantMenu(little.token), (error) => error instanceof ApiError && error.status === 404);

      const littleMenu = emptyRestaurantMenu(4, 'Little Italy Pizza');
      littleMenu.menuName = 'Little Italy Menu';
      littleMenu.items = [{ id: null, name: 'Margherita pizza', description: '', category: 'Pizza', price: 17, status: 'available', photoUrl: null, options: [] }];
      await saveRestaurantMenu(little.token, littleMenu);
      assert.equal((await loadRestaurantMenu(little.token)).items[0].name, 'Margherita pizza');
      assert.equal((await loadRestaurantMenu(green.token)).items.length, 3);
      await assert.rejects(saveRestaurantMenu(green.token, littleMenu), /does not belong/);

      const greenQueue = loadRestaurantOrders(2);
      assert.equal(greenQueue.length, 11);
      assert.deepEqual(filterAndSortOrders(greenQueue, 'placed').map((order) => order.id), [9301, 9302, 9303]);
      assert.deepEqual(loadRestaurantOrders(4), []);

      const accepted = transitionOrder(greenQueue[0], 'accepted', '2026-10-23T14:30:00.000Z');
      saveRestaurantOrders(2, greenQueue.map((order) => order.id === accepted.id ? accepted : order));
      assert.equal(loadRestaurantOrders(2).find((order) => order.id === accepted.id)?.status, 'accepted');
      saveRestaurantOrders(2, deleteSelectedOrders(loadRestaurantOrders(2), new Set([9301, 9302])));
      assert.equal(loadRestaurantOrders(2).length, 9);
      assert.deepEqual(loadRestaurantOrders(4), []);
      assert.ok([...values.keys()].some((key) => key.includes('menu-v2:4')));

      await logout(green.token);
      await logout(little.token);
      assert.equal(fetchMock.mock.callCount(), 2, 'only the two login attempts should touch the unreachable backend');
    } finally {
      if (previousStorage) Object.defineProperty(globalThis, 'localStorage', previousStorage);
      else Reflect.deleteProperty(globalThis, 'localStorage');
    }
  });

  it('does not turn wrong credentials or another role into an offline demo login', async () => {
    mock.method(globalThis, 'fetch', async () => { throw new TypeError('Backend offline'); });
    await assert.rejects(login({ role: 'restaurant', email: 'greenbowl@demo.test', password: 'wrong' }), /Cannot reach the server/);
    await assert.rejects(login({ role: 'customer', email: 'greenbowl@demo.test', password: 'DemoPass123!' }), /Cannot reach the server/);
  });
});
