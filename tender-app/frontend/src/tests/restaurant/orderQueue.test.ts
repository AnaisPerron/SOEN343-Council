import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { restaurantOrdersMock } from '../../mocks/restaurantOrders.ts';
import { deleteSelectedOrders, filterAndSortOrders, ORDER_STATUSES, orderTotal, transitionOrder } from '../../services/restaurant/orderQueue.ts';

describe('restaurant orders queue', () => {
  it('puts placed orders first, oldest first, and filters every status', () => {
    const sourceOrder = restaurantOrdersMock.map((order) => order.id);
    const all = filterAndSortOrders(restaurantOrdersMock, 'all');
    assert.deepEqual(all.slice(0, 3).map((order) => order.id), [9301, 9302, 9303]);
    for (const status of ORDER_STATUSES) {
      const filtered = filterAndSortOrders(restaurantOrdersMock, status);
      assert.ok(filtered.length > 0, `${status} should have a mock case`);
      assert.ok(filtered.every((order) => order.status === status));
    }
    assert.deepEqual(restaurantOrdersMock.map((order) => order.id), sourceOrder, 'sorting must not mutate the fixture');
  });

  it('accepts, starts preparation, and marks an order ready', () => {
    const placed = restaurantOrdersMock.find((order) => order.id === 9301)!;
    const accepted = transitionOrder(placed, 'accepted', '2026-10-23T14:20:00.000Z');
    assert.equal(accepted.acceptedAt, '2026-10-23T14:20:00.000Z');
    const preparing = transitionOrder(accepted, 'preparing', '2026-10-23T14:25:00.000Z');
    const ready = transitionOrder(preparing, 'ready', '2026-10-23T14:35:00.000Z');
    assert.equal(ready.status, 'ready');
    assert.equal(placed.status, 'placed', 'the original order must remain unchanged');
    assert.throws(() => transitionOrder(placed, 'ready', '2026-10-23T14:40:00.000Z'));
    assert.throws(() => transitionOrder(accepted, 'ready', '2026-10-23T14:40:00.000Z'));
    assert.throws(() => transitionOrder(ready, 'accepted', '2026-10-23T14:40:00.000Z'));
  });

  it('supports an optional rejection reason and deletes multiple selected orders', () => {
    const placed = restaurantOrdersMock[0];
    assert.equal(transitionOrder(placed, 'rejected', '2026-10-23T14:20:00.000Z', '  ').rejectionReason, null);
    assert.equal(transitionOrder(placed, 'rejected', '2026-10-23T14:20:00.000Z').rejectionReason, null);
    const rejected = transitionOrder(placed, 'rejected', '2026-10-23T14:20:00.000Z', ' Ingredient unavailable ');
    assert.equal(rejected.rejectionReason, 'Ingredient unavailable');
    assert.equal(rejected.acceptedAt, null);
    const remaining = deleteSelectedOrders(restaurantOrdersMock, new Set([9301, 9302]));
    assert.equal(remaining.length, restaurantOrdersMock.length - 2);
    assert.equal(remaining.some((order) => order.id === 9301 || order.id === 9302), false);
    assert.equal(restaurantOrdersMock.length, 11, 'deletion must not mutate the source queue');
    assert.equal(orderTotal(placed), 26);
  });
});
