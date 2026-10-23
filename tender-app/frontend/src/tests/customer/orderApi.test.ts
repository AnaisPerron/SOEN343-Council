import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { sendOrder, sendOrderCancellation, toOrderSubmission, type OrderSubmission } from '../../app/customer/services/orderApi.ts';

const order: OrderSubmission = {
  restaurantId: 1,
  items: [{ menuItemId: 1001, quantity: 2, optionIds: [5001] }],
};

const receipt = { orderId: 9006, restaurantId: 1, status: 'placed', total: 33, placedAt: '2026-10-29T12:00:00Z', paymentReference: 'MOCK-1', lines: [] };

describe('order backend request', () => {
  it('maps a restaurant cart group to the backend order body', () => {
    const submission = toOrderSubmission({
      restaurantId: 1,
      lines: [{ key: '1001:5001', itemId: 1001, restaurantId: 1, optionIds: [5001], quantity: 2, unitPrice: 16.5 }],
      subtotal: 33, deliveryFee: 0, serviceFee: 0, total: 33,
    });
    assert.deepEqual(submission, order);
  });

  it('sends the order as JSON with the session token and returns the server receipt', async () => {
    let requestedUrl = '';
    let requestedOptions: RequestInit | undefined;
    const fakeFetch: typeof fetch = async (url, options) => {
      requestedUrl = String(url);
      requestedOptions = options;
      return new Response(JSON.stringify(receipt), { status: 201, headers: { 'Content-Type': 'application/json' } });
    };

    const result = await sendOrder('https://api.example.test/api/orders', 'token-1', order, fakeFetch);
    assert.equal(requestedUrl, 'https://api.example.test/api/orders');
    assert.equal(requestedOptions?.method, 'POST');
    assert.deepEqual(requestedOptions?.headers, { 'Content-Type': 'application/json', Authorization: 'Bearer token-1' });
    assert.deepEqual(JSON.parse(String(requestedOptions?.body)), order);
    assert.equal(result.orderId, 9006);
    assert.equal(result.total, 33);
  });

  it('reports the backend message for a rejected order', async () => {
    const fakeFetch: typeof fetch = async () => new Response(JSON.stringify({ message: 'Restaurant 1 : Metro Pizza is not open' }), { status: 409 });
    await assert.rejects(sendOrder('https://api.example.test/api/orders', 'token-1', order, fakeFetch), /Metro Pizza is not open/);
  });

  it('falls back to the HTTP status when the response has no message', async () => {
    const fakeFetch: typeof fetch = async () => new Response('Restaurant closed', { status: 409 });
    await assert.rejects(sendOrder('https://api.example.test/api/orders', 'token-1', order, fakeFetch), /Order submission failed \(409\)/);
  });

  it('propagates network failures to the caller', async () => {
    const fakeFetch: typeof fetch = async () => { throw new Error('Network unavailable'); };
    await assert.rejects(sendOrder('https://api.example.test/api/orders', 'token-1', order, fakeFetch), /Network unavailable/);
  });

  it('cancels an order without a request body', async () => {
    let requestedOptions: RequestInit | undefined;
    const fakeFetch: typeof fetch = async (_url, options) => {
      requestedOptions = options;
      return new Response(JSON.stringify({ orderId: 9006, previousStatus: 'placed', status: 'cancelled', changedAt: '2026-10-29T12:01:00Z' }), { status: 200 });
    };
    const result = await sendOrderCancellation('https://api.example.test/api/orders/9006/cancel', 'token-1', fakeFetch);
    assert.equal(requestedOptions?.body, undefined);
    assert.equal(result.status, 'cancelled');
  });
});
