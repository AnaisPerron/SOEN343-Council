import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { sendOrderRating } from '../../app/customer/services/orderApi.ts';
import { canRateOrder, formatAverageRating, ratingFormProblem, toRatingSubmission } from '../../app/customer/services/orderRating.ts';

const json = (body: unknown, status: number) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

describe('order rating', () => {
  it('offers the rating action only for delivered orders', () => {
    assert.equal(canRateOrder('delivered'), true);
    for (const status of ['placed', 'accepted', 'preparing', 'ready', 'picked_up', 'rejected', 'cancelled', 'refunded']) {
      assert.equal(canRateOrder(status), false, status);
    }
  });

  it('sends courier stars only when a courier delivered and drops a blank comment', () => {
    assert.deepEqual(toRatingSubmission(5, 4, '  Hot and on time ', true), { restaurantStars: 5, courierStars: 4, comment: 'Hot and on time' });
    assert.deepEqual(toRatingSubmission(3, 4, '   ', false), { restaurantStars: 3 });
  });

  it('asks for the missing stars before sending', () => {
    assert.equal(ratingFormProblem(null, null, false), 'Choose a rating for the restaurant.');
    assert.equal(ratingFormProblem(4, null, true), 'Choose a rating for the courier.');
    assert.equal(ratingFormProblem(4, null, false), null);
    assert.equal(ratingFormProblem(4, 5, true), null);
  });

  it('formats the restaurant average and the no-ratings case', () => {
    assert.equal(formatAverageRating(4.25, 4), '4.3 (4 ratings)');
    assert.equal(formatAverageRating(5, 1), '5.0 (1 rating)');
    assert.equal(formatAverageRating(null, 0), 'No ratings yet');
    assert.equal(formatAverageRating(undefined, undefined), 'No ratings yet');
  });

  it('posts the rating with the session token', async () => {
    let requestedUrl = '';
    let requestedOptions: RequestInit | undefined;
    const saved = { id: 1, orderId: 7, restaurantId: 2, courierId: 7, restaurantStars: 5, courierStars: 4, comment: null, createdAt: '2026-11-25T12:00:00Z' };
    const fakeFetch: typeof fetch = async (url, options) => {
      requestedUrl = String(url);
      requestedOptions = options;
      return json(saved, 201);
    };
    const result = await sendOrderRating('http://api/api/orders/7/rating', 'token-1', { restaurantStars: 5, courierStars: 4 }, fakeFetch);
    assert.deepEqual(result, saved);
    assert.equal(requestedUrl, 'http://api/api/orders/7/rating');
    assert.equal(requestedOptions?.method, 'POST');
    assert.equal((requestedOptions?.headers as Record<string, string>).Authorization, 'Bearer token-1');
    assert.deepEqual(JSON.parse(String(requestedOptions?.body)), { restaurantStars: 5, courierStars: 4 });
  });

  it('surfaces the refusal message for a second rating', async () => {
    const fakeFetch: typeof fetch = async () => json({ status: 409, message: 'Order 7 has already been rated', fieldErrors: {} }, 409);
    await assert.rejects(sendOrderRating('http://api/api/orders/7/rating', 't', { restaurantStars: 5 }, fakeFetch),
      { message: 'Order 7 has already been rated' });
  });
});
