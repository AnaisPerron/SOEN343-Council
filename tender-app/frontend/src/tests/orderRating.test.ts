import assert from 'node:assert/strict';
import { afterEach, describe, it, mock } from 'node:test';

import { API_URL } from '../api/client.ts';
import { fetchOrderRating } from '../api/orderRating.ts';

describe('order rating API', () => {
  afterEach(() => mock.restoreAll());

  it('fetches the signed-in user’s rating for an order', async () => {
    const rating = { orderId: 42, stars: 4, comment: 'Hot and on time', createdAt: '2026-11-25T12:00:00Z' };
    const fetchMock = mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify(rating), { status: 200 }));
    assert.deepEqual(await fetchOrderRating(42, 'session-token'), rating);

    const [url, request] = fetchMock.mock.calls[0].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/orders/42/rating`);
    assert.equal(request.method, 'GET');
    assert.equal((request.headers as Record<string, string>).Authorization, 'Bearer session-token');
  });

  it('returns null when the order has not been rated and propagates access failures', async () => {
    mock.method(globalThis, 'fetch', async () => new Response(null, { status: 204 }));
    assert.equal(await fetchOrderRating(42, 'session-token'), null);
    mock.restoreAll();

    mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify({ message: 'Forbidden' }), { status: 403 }));
    await assert.rejects(fetchOrderRating(42, 'session-token'), /Forbidden/);
  });
});
