import assert from 'node:assert/strict';
import { afterEach, describe, it, mock } from 'node:test';

import { API_URL } from '../../api/client.ts';
import { acceptCourierOffer, fetchCourierDuty, goOffDuty, goOnDuty } from '../../api/courierDuty.ts';
import { canGoOffDuty, canGoOnDuty, isOnDuty } from '../../lib/courierDuty.ts';

describe('courier duty toggle', () => {
  it('only allows going off duty while idle', () => {
    assert.equal(canGoOffDuty('idle'), true);
    assert.equal(canGoOffDuty('assigned'), false);
    assert.equal(canGoOffDuty('carrying'), false);
    assert.equal(canGoOffDuty('offline'), false);
  });

  it('only allows going on duty while offline', () => {
    assert.equal(canGoOnDuty('offline'), true);
    assert.equal(canGoOnDuty('idle'), false);
  });

  it('treats every state but offline as on duty', () => {
    assert.equal(isOnDuty('offline'), false);
    for (const state of ['idle', 'assigned', 'carrying'] as const) assert.equal(isOnDuty(state), true);
  });
});

describe('courier duty API', () => {
  afterEach(() => mock.restoreAll());

  it('reads and changes duty status with the session token', async () => {
    const duty = { courierId: 6, status: 'idle', latitude: null, longitude: null, assignedOrder: null };
    const fetchMock = mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify(duty), { status: 200 }));

    assert.deepEqual(await fetchCourierDuty('session-token'), duty);
    await goOnDuty('session-token');
    await goOffDuty('session-token');

    const calls = fetchMock.mock.calls.map((call) => call.arguments as [string, RequestInit]);
    assert.deepEqual(calls.map(([url, request]) => [request.method, url]), [
      ['GET', `${API_URL}/api/courier/duty`],
      ['POST', `${API_URL}/api/courier/duty/on`],
      ['POST', `${API_URL}/api/courier/duty/off`],
    ]);
    for (const [, request] of calls) assert.equal((request.headers as Record<string, string>).Authorization, 'Bearer session-token');
  });
});


describe('courier offer acceptance API', () => {
  afterEach(() => mock.restoreAll());

  it('accepts the specific assignment using the session token without a caller-supplied courier id', async () => {
    const offer = { assignmentId: 41, assignmentStatus: 'accepted', assignedAt: '2026-11-12T12:00:00Z',
      acceptedAt: '2026-11-12T12:01:00Z', orderId: 7, status: 'ready', restaurantName: 'Cafe',
      restaurantAddress: '123 Pickup St', deliveryAddress: '456 Delivery St' };
    const fetchMock = mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify(offer), { status: 200 }));
    assert.deepEqual(await acceptCourierOffer(41, 'courier-token'), offer);
    const [url, request] = fetchMock.mock.calls[0].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/courier/offers/41/accept`);
    assert.equal(request.method, 'POST');
    assert.equal((request.headers as Record<string, string>).Authorization, 'Bearer courier-token');
    assert.equal(request.body, undefined);
  });

  it('preserves expired-session, ownership and stale-offer failures', async () => {
    for (const status of [401, 403, 404, 409]) {
      const fetchMock = mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify({ message: 'Offer unavailable' }), { status }));
      await assert.rejects(acceptCourierOffer(41, 'courier-token'), (error: { status: number }) => error.status === status);
      fetchMock.mock.restore();
    }
  });
});
