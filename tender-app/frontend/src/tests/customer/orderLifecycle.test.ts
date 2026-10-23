import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { addCartLine, groupCartLinesByRestaurant } from '../../app/customer/cart/cartStore.ts';
import { cancelPlacedOrder, createPlacedOrders } from '../../app/customer/services/orderLifecycle.ts';
import { menuItemsMock } from '../../mocks/menus.mock.ts';

describe('local order lifecycle', () => {
  it('creates a separate placed order for each restaurant from the cart', () => {
    const first = addCartLine([], menuItemsMock[0], 1, [5001], 2, 2);
    const lines = addCartLine(first, menuItemsMock[6], 2, [], 0, 1);
    const groups = groupCartLinesByRestaurant(lines);
    const orders = createPlacedOrders(groups, '2026-10-23T12:00:00.000Z', 7);
    assert.deepEqual(orders.map((order) => ({ id: order.id, restaurantId: order.restaurantId, status: order.status, total: order.total })), [
      { id: '7', restaurantId: 1, status: 'placed', total: 33 },
      { id: '8', restaurantId: 2, status: 'placed', total: 15.75 },
    ]);
    assert.deepEqual(orders[0].lines[0].optionIds, [5001]);
    assert.notStrictEqual(orders[0].lines[0], lines[0]);
  });

  it('allows cancellation only while an order is placed', () => {
    const group = groupCartLinesByRestaurant(addCartLine([], menuItemsMock[0], 1, [], 0, 1));
    const placed = createPlacedOrders(group, '2026-10-23T12:00:00.000Z', 1)[0];
    const cancelled = cancelPlacedOrder(placed);
    assert.equal(cancelled.status, 'cancelled');
    assert.equal(placed.status, 'placed');
    assert.strictEqual(cancelPlacedOrder(cancelled), cancelled);
    assert.strictEqual(cancelPlacedOrder({ ...placed, status: 'accepted' }).status, 'accepted');
  });
});
