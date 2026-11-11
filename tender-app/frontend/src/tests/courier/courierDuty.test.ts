import assert from 'node:assert/strict';
import { afterEach, describe, it, mock } from 'node:test';

import { API_URL } from '../../api/client.ts';
import { fetchCourierDuty, goOffDuty, goOnDuty } from '../../api/courierDuty.ts';
import { fetchLedgerHistory } from '../../api/orderHistory.ts';
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

  it('loads only the server-filtered courier history', async () => {
    const record = { orderId: 12, status: 'DELIVERED', placedAt: '2026-11-11T10:00:00Z', pickedUpAt: null, deliveredAt: '2026-11-11T11:00:00Z', totalCharge: 14.1 };
    const fetchMock = mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify([record]), { status: 200 }));
    assert.deepEqual(await fetchLedgerHistory('session-token'), [record]);
    assert.equal(fetchMock.mock.calls[0].arguments[0], `${API_URL}/api/pricing/history`);
    assert.equal((fetchMock.mock.calls[0].arguments[1] as RequestInit).method, 'GET');
  });
});
