import assert from 'node:assert/strict';
import { afterEach, describe, it, mock } from 'node:test';
import { API_URL, ApiError } from '../../api/client.ts';
import { appendOperatorSection, fetchOperatorSection, fetchOperatorSummary } from '../../lib/operatorDashboard.ts';
import { changeRestaurantStatus } from '../../app/operator/operatorApi.ts';
import { queueBand } from '../../app/operator/operatorLogic.ts';
import type { OperatorSection } from '../../types/operator.ts';

const restaurant = { id: 1, name: 'Live Restaurant', status: 'open', latitude: null, longitude: null, address: null, phone: null, queueCount: 0 };
const envelope = (items: unknown[], page = 0, total = items.length) => ({ items, page, total, size: 50 });
function response(body: unknown, status = 200) {
  return mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify(body), { status }));
}

describe('operator dashboard data', () => {
  afterEach(() => mock.restoreAll());

  it('uses the operator token and requested page for restaurant reads', async () => {
    const fetch = response(envelope([restaurant], 1, 51));
    const data = await fetchOperatorSection('restaurants', 1, 'operator-token');
    const [url, init] = fetch.mock.calls[0].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/operator/restaurants?page=1&size=50`);
    assert.equal(init.method, 'GET');
    assert.equal((init.headers as Record<string, string>).Authorization, 'Bearer operator-token');
    assert.deepEqual(data, { kind: 'restaurants', data: envelope([restaurant], 1, 51) });
  });

  it('keeps complete summary counts separate from loaded pages and available couriers', async () => {
    const counts = { restaurantCount: 101, orderCount: 200, customerCount: 30, courierCount: 4, availableCourierCount: 1 };
    const fetch = response(counts);
    assert.deepEqual(await fetchOperatorSummary('token'), counts);
    assert.equal(fetch.mock.calls[0].arguments[0], `${API_URL}/api/operator/summary`);
  });

  it('preserves unknown customer balances and courier availability', async () => {
    const account = { id: 8, name: 'Account', email: 'a@example.test', phone: null, latitude: null, longitude: null };
    const fetch = response(envelope([{ ...account, tdCashBalance: null }]));
    const customers = await fetchOperatorSection('customers', 0, 'token');
    assert.equal(customers.kind, 'customers');
    if (customers.kind === 'customers') assert.equal(customers.data.items[0].tdCashBalance, null);
    fetch.mock.mockImplementation(async () => new Response(JSON.stringify(envelope([{ ...account, status: null }]))));
    const couriers = await fetchOperatorSection('couriers', 0, 'token');
    assert.equal(couriers.kind, 'couriers');
    if (couriers.kind === 'couriers') assert.equal(couriers.data.items[0].status, null);
  });

  it('reads refunded orders and backend totals without mock order lines', async () => {
    const order = { id: 9, customerId: null, restaurantId: null, courierId: null, status: 'refunded',
      dateOrdered: null, acceptedAt: null, rejectionReason: null, totalPrice: 42.75 };
    response(envelope([order]));
    const result = await fetchOperatorSection('orders', 0, 'token');
    assert.deepEqual(result.data.items, [order]);
  });

  it('sends status, restaurant, and date filters with every order page', async () => {
    const fetch = response(envelope([], 2));
    await fetchOperatorSection('orders', 2, 'token', {
      statuses: ['delivered', 'refunded'], restaurantIds: [4, 8], from: '2026-11-01', to: '2026-11-11',
    });
    const url = new URL(fetch.mock.calls[0].arguments[0] as string);
    assert.equal(url.pathname, '/api/operator/orders');
    assert.equal(url.searchParams.get('page'), '2');
    assert.deepEqual(url.searchParams.getAll('statuses'), ['delivered', 'refunded']);
    assert.deepEqual(url.searchParams.getAll('restaurantIds'), ['4', '8']);
    assert.equal(url.searchParams.get('from'), '2026-11-01');
    assert.equal(url.searchParams.get('to'), '2026-11-11');
    assert.equal(url.searchParams.get('timeZone'), Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC');
  });

  it('uses R-DASH-03 queue thresholds and paused override', () => {
    assert.equal(queueBand(0, 'open'), 'short');
    assert.equal(queueBand(2, 'open'), 'short');
    assert.equal(queueBand(3, 'open'), 'building');
    assert.equal(queueBand(5, 'open'), 'building');
    assert.equal(queueBand(6, 'open'), 'long');
    assert.equal(queueBand(0, 'paused'), 'long');
  });

  it('accepts empty pages instead of substituting fixtures', async () => {
    response(envelope([]));
    const result = await fetchOperatorSection('restaurants', 0, 'token');
    assert.deepEqual(result.data.items, []);
    assert.equal(result.data.total, 0);
  });

  it('rejects invalid rows and mismatched pagination metadata', async () => {
    const fetch = response(envelope([{ ...restaurant, status: ['open'] }]));
    await assert.rejects(fetchOperatorSection('restaurants', 0, 'token'), (e: ApiError) => e.status === 502);
    for (const data of [envelope([restaurant], 1), { ...envelope([restaurant]), total: -1 }, { items: [] }]) {
      fetch.mock.mockImplementation(async () => new Response(JSON.stringify(data)));
      await assert.rejects(fetchOperatorSection('restaurants', 0, 'token'), (e: ApiError) => e.status === 502);
    }
  });

  it('rejects malformed summary counts', async () => {
    response({ restaurantCount: '4' });
    await assert.rejects(fetchOperatorSummary('token'), (e: ApiError) => e.status === 502);
  });

  it('preserves session and permission failures for the dashboard to handle', async () => {
    const fetch = response({ message: 'Session expired' }, 401);
    await assert.rejects(fetchOperatorSection('restaurants', 0, 'token'), (e: ApiError) => e.status === 401);
    fetch.mock.mockImplementation(async () => new Response(JSON.stringify({ message: 'Forbidden' }), { status: 403 }));
    await assert.rejects(fetchOperatorSummary('token'), (e: ApiError) => e.status === 403);
  });

  it('reports a network failure without returning fake data', async () => {
    mock.method(globalThis, 'fetch', async () => { throw new Error('offline'); });
    await assert.rejects(fetchOperatorSection('orders', 0, 'token'), (e: ApiError) => e.status === 0);
  });

  it('appends only matching consecutive pages and deduplicates repeated ids', () => {
    const current: OperatorSection = { kind: 'restaurants', data: envelope([restaurant]) } as OperatorSection;
    const next: OperatorSection = { kind: 'restaurants', data: envelope([restaurant, { ...restaurant, id: 2 }], 1, 51) } as OperatorSection;
    const combined = appendOperatorSection(current, next);
    assert.deepEqual(combined.data.items.map((item) => item.id), [1, 2]);
    assert.equal(current.data.items.length, 1);
    const different: OperatorSection = { kind: 'orders', data: envelope([]) };
    assert.deepEqual(appendOperatorSection(current, different), different);
    const skipped = { ...next, data: { ...next.data, page: 3 } } as OperatorSection;
    assert.deepEqual(appendOperatorSection(current, skipped), skipped);
  });

  it('supports successful no-content status updates with the operator token', async () => {
    const fetch = mock.method(globalThis, 'fetch', async () => new Response(null, { status: 204 }));
    await changeRestaurantStatus(1, 'paused', 'token');
    const [url, init] = fetch.mock.calls[0].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/operator/restaurants/1/status`);
    assert.equal(init.method, 'PUT');
    assert.equal((init.headers as Record<string, string>).Authorization, 'Bearer token');
    assert.deepEqual(JSON.parse(init.body as string), { status: 'paused' });
  });

  it('does not treat a rejected status update as successful', async () => {
    response({ message: 'Restaurant no longer exists' }, 404);
    await assert.rejects(changeRestaurantStatus(1, 'open', 'token'), (e: ApiError) => e.status === 404);
  });
});
