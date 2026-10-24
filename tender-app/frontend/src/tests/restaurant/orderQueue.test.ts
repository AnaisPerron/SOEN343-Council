import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { restaurantOrdersMock } from '../../mocks/restaurantOrders.ts';
import { deleteSelectedOrders, filterAndSortOrders, orderTotal, transitionOrder } from '../../services/restaurant/orderQueue.ts';

describe('restaurant orders queue', () => {
  it('puts placed orders first, oldest first, and filters every status', () => {
    const all = filterAndSortOrders(restaurantOrdersMock, 'all');
    assert.deepEqual(all.slice(0, 3).map((order) => order.id), [9301, 9302, 9303]);
    assert.deepEqual(filterAndSortOrders(restaurantOrdersMock, 'refunded').map((order) => order.id), [9311]);
  });

  it('accepts, starts preparation, and marks an order ready', () => {
    const placed = restaurantOrdersMock.find((order) => order.id === 9301)!;
    const accepted = transitionOrder(placed, 'accepted', '2026-10-23T14:20:00.000Z');
    assert.equal(accepted.acceptedAt, '2026-10-23T14:20:00.000Z');
    const preparing = transitionOrder(accepted, 'preparing', '2026-10-23T14:25:00.000Z');
    const ready = transitionOrder(preparing, 'ready', '2026-10-23T14:35:00.000Z');
    assert.equal(ready.status, 'ready');
    assert.throws(() => transitionOrder(ready, 'accepted', '2026-10-23T14:40:00.000Z'));
  });

  it('requires a reason for rejection and deletes multiple selected orders', () => {
    const placed = restaurantOrdersMock[0];
    assert.throws(() => transitionOrder(placed, 'rejected', '2026-10-23T14:20:00.000Z', '  '), /reason/);
    const rejected = transitionOrder(placed, 'rejected', '2026-10-23T14:20:00.000Z', ' Ingredient unavailable ');
    assert.equal(rejected.rejectionReason, 'Ingredient unavailable');
    const remaining = deleteSelectedOrders(restaurantOrdersMock, new Set([9301, 9302]));
    assert.equal(remaining.length, restaurantOrdersMock.length - 2);
    assert.equal(remaining.some((order) => order.id === 9301 || order.id === 9302), false);
    assert.equal(orderTotal(placed), 26);
  });
});
