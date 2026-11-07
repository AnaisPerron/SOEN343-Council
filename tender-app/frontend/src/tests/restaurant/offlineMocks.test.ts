import assert from 'node:assert/strict';
import { afterEach, describe, it, mock } from 'node:test';

import { login } from '../../api/auth.ts';
import { API_URL, ApiError } from '../../api/client.ts';
import { createMenuItem, listMenuItems, toggleMenuItemAvailability, updateMenuItem, uploadMenuPhoto, type MenuItem } from '../../api/restaurantMenu.ts';

const dto = { id: 7, menu_id: 2, name: 'Margherita', price: 14.5, description: null, available: true, photoUrl: null, category: 'Pizza' };
const item: MenuItem = { id: 7, name: 'Margherita', description: '', category: 'Pizza', price: 14.5, available: true, photoUrl: null };

describe('restaurant menu API', () => {
  afterEach(() => mock.restoreAll());

  it('lists, creates, updates and toggles menu items with the authenticated session', async () => {
    const fetchMock = mock.method(globalThis, 'fetch', async (url: string) =>
      new Response(JSON.stringify(url.endsWith('/menu-items') && fetchMock.mock.callCount() === 0 ? [dto] : dto), { status: 200 }));

    assert.deepEqual(await listMenuItems('session-token'), [item]);
    assert.deepEqual(await createMenuItem('session-token', { ...item, id: null }), item);
    assert.deepEqual(await updateMenuItem('session-token', 7, item), item);
    assert.deepEqual(await toggleMenuItemAvailability('session-token', 7), item);

    const calls = fetchMock.mock.calls.map((call) => call.arguments as [string, RequestInit]);
    assert.deepEqual(calls.map(([url, request]) => [request.method, url]), [
      ['GET', `${API_URL}/api/restaurant/menu-items`],
      ['POST', `${API_URL}/api/restaurant/menu-items`],
      ['PUT', `${API_URL}/api/restaurant/menu-items/7`],
      ['PATCH', `${API_URL}/api/restaurant/menu-items/7/availability`],
    ]);
    for (const [, request] of calls) assert.equal((request.headers as Record<string, string>).Authorization, 'Bearer session-token');
    assert.deepEqual(JSON.parse(calls[1][1].body as string),
      { name: 'Margherita', description: '', price: 14.5, available: true, category: 'Pizza', photoUrl: null });
  });

  it('uploads a picked photo and returns the backend path', async () => {
    const fetchMock = mock.method(globalThis, 'fetch', async () =>
      new Response(JSON.stringify({ url: '/menu-photos/3' }), { status: 201 }));

    assert.equal(await uploadMenuPhoto('session-token', 'data:image/jpeg;base64,/9j/'), '/menu-photos/3');

    const [url, request] = fetchMock.mock.calls[0].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/restaurant/menu-items/photos`);
    assert.equal(request.method, 'POST');
    assert.deepEqual(JSON.parse(request.body as string), { dataUri: 'data:image/jpeg;base64,/9j/' });
  });

  it('reports backend menu errors', async () => {
    mock.method(globalThis, 'fetch', async () =>
      new Response(JSON.stringify({ message: 'Restaurant has no menu' }), { status: 404 }));
    await assert.rejects(listMenuItems('session-token'),
      (error) => error instanceof ApiError && error.status === 404 && error.message === 'Restaurant has no menu');
  });

  it('reports when authentication and menu requests cannot reach the backend', async () => {
    mock.method(globalThis, 'fetch', async () => { throw new TypeError('Backend offline'); });
    await assert.rejects(login({ role: 'restaurant', email: 'greenbowl@demo.test', password: 'DemoPass123!' }), /Cannot reach the server/);
    await assert.rejects(listMenuItems('session-token'),
      (error) => error instanceof ApiError && error.status === 0);
    await assert.rejects(createMenuItem('session-token', item),
      (error) => error instanceof ApiError && error.status === 0);
  });
});
