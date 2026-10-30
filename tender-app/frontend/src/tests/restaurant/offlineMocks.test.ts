import assert from 'node:assert/strict';
import { afterEach, describe, it, mock } from 'node:test';

import { login } from '../../api/auth.ts';
import { API_URL, ApiError } from '../../api/client.ts';
import { loadRestaurantMenu, saveRestaurantMenu } from '../../api/restaurantMenu.ts';
import { emptyRestaurantMenu } from '../../mocks/restaurantMenu.ts';

describe('restaurant menu API', () => {
  afterEach(() => mock.restoreAll());

  it('loads and saves a menu with the authenticated session', async () => {
    const menu = emptyRestaurantMenu(4, 'Little Italy Pizza');
    const fetchMock = mock.method(globalThis, 'fetch', async () =>
      new Response(JSON.stringify(menu), { status: 200 }));

    assert.deepEqual(await loadRestaurantMenu('session-token'), menu);
    assert.deepEqual(await saveRestaurantMenu('session-token', menu), menu);

    const [getUrl, getRequest] = fetchMock.mock.calls[0].arguments as [string, RequestInit];
    const [putUrl, putRequest] = fetchMock.mock.calls[1].arguments as [string, RequestInit];
    assert.equal(getUrl, `${API_URL}/api/restaurant/menu`);
    assert.equal(putUrl, getUrl);
    assert.equal(getRequest.method, 'GET');
    assert.equal(putRequest.method, 'PUT');
    assert.equal((getRequest.headers as Record<string, string>).Authorization, 'Bearer session-token');
    assert.equal((putRequest.headers as Record<string, string>).Authorization, 'Bearer session-token');
    assert.deepEqual(JSON.parse(putRequest.body as string), menu);
  });

  it('reports backend menu errors', async () => {
    mock.method(globalThis, 'fetch', async () =>
      new Response(JSON.stringify({ message: 'Menu not found' }), { status: 404 }));
    await assert.rejects(loadRestaurantMenu('session-token'),
      (error) => error instanceof ApiError && error.status === 404 && error.message === 'Menu not found');
  });

  it('reports when authentication and menu requests cannot reach the backend', async () => {
    mock.method(globalThis, 'fetch', async () => { throw new TypeError('Backend offline'); });
    await assert.rejects(login({ role: 'restaurant', email: 'greenbowl@demo.test', password: 'DemoPass123!' }), /Cannot reach the server/);
    await assert.rejects(loadRestaurantMenu('session-token'),
      (error) => error instanceof ApiError && error.status === 0);
    await assert.rejects(saveRestaurantMenu('session-token', emptyRestaurantMenu(4, 'Little Italy Pizza')),
      (error) => error instanceof ApiError && error.status === 0);
  });
});
