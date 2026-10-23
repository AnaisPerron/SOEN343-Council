import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { sendOrder, type OrderSubmission } from '../../app/customer/services/orderApi.ts';

const order: OrderSubmission = {
  restaurantId: 1,
  lineItems: [{ menuItemId: 1001, quantity: 2, optionIds: [5001] }],
  paymentChoice: 'card',
};

describe('order backend request', () => {
  it('sends the order as JSON to the provided endpoint and returns the server result', async () => {
    let requestedUrl = '';
    let requestedOptions: RequestInit | undefined;
    const fakeFetch: typeof fetch = async (url, options) => {
      requestedUrl = String(url);
      requestedOptions = options;
      return new Response(JSON.stringify({ orderId: 9006, status: 'placed' }), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      });
    };

    const result = await sendOrder('https://api.example.test/orders', order, fakeFetch);
    assert.equal(requestedUrl, 'https://api.example.test/orders');
    assert.equal(requestedOptions?.method, 'POST');
    assert.deepEqual(requestedOptions?.headers, { 'Content-Type': 'application/json' });
    assert.deepEqual(JSON.parse(String(requestedOptions?.body)), order);
    assert.deepEqual(result, { orderId: 9006, status: 'placed' });
  });

  it('reports a rejected HTTP response without treating it as an order', async () => {
    const fakeFetch: typeof fetch = async () => new Response('Restaurant closed', { status: 409 });
    await assert.rejects(sendOrder('https://api.example.test/orders', order, fakeFetch), /Order submission failed \(409\)/);
  });

  it('propagates network failures to the caller', async () => {
    const fakeFetch: typeof fetch = async () => { throw new Error('Network unavailable'); };
    await assert.rejects(sendOrder('https://api.example.test/orders', order, fakeFetch), /Network unavailable/);
  });
});
