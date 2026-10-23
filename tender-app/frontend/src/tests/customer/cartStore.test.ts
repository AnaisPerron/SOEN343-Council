import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { menuItemsMock } from '../../mocks/menus.mock.ts';
import { addCartLine, cartTotal, changeCartLineQuantity, groupCartLinesByRestaurant } from '../../app/customer/cartStore.ts';

describe('cart draft', () => {
  it('adds selected extras and quantity to the total', () => {
    const lines = addCartLine([], menuItemsMock[0], 1, [5002, 5001], 3.5, 2);
    assert.deepEqual(lines[0].optionIds, [5001, 5002]);
    assert.equal(lines[0].quantity, 2);
    assert.equal(cartTotal(lines), 36);
  });

  it('combines the same item and extras but keeps a different selection separate', () => {
    const first = addCartLine([], menuItemsMock[0], 1, [5001], 2, 1);
    const combined = addCartLine(first, menuItemsMock[0], 1, [5001], 2, 2);
    const separate = addCartLine(combined, menuItemsMock[0], 1, [], 0, 1);
    assert.equal(separate.length, 2);
    assert.equal(separate[0].quantity, 3);
  });

  it('rejects unavailable items and removes a line when its quantity reaches zero', () => {
    assert.deepEqual(addCartLine([], menuItemsMock[2], 1, [], 0, 1), []);
    const lines = addCartLine([], menuItemsMock[0], 1, [], 0, 1);
    assert.deepEqual(changeCartLineQuantity(lines, lines[0].key, 0), []);
  });

  it('keeps each restaurant subtotal and placeholder fees separate', () => {
    const first = addCartLine([], menuItemsMock[0], 1, [], 0, 2);
    const both = addCartLine(first, menuItemsMock[6], 2, [], 0, 1);
    const groups = groupCartLinesByRestaurant(both);
    assert.deepEqual(groups.map((group) => ({ restaurantId: group.restaurantId, subtotal: group.subtotal, deliveryFee: group.deliveryFee, serviceFee: group.serviceFee, total: group.total })), [
      { restaurantId: 1, subtotal: 29, deliveryFee: 0, serviceFee: 0, total: 29 },
      { restaurantId: 2, subtotal: 15.75, deliveryFee: 0, serviceFee: 0, total: 15.75 },
    ]);
  });

  it('updates only the selected line and keeps its chosen unit price', () => {
    const first = addCartLine([], menuItemsMock[0], 1, [5001], 2, 1);
    const lines = addCartLine(first, menuItemsMock[0], 1, [], 0, 1);
    const updated = changeCartLineQuantity(lines, first[0].key, 3);
    assert.equal(updated[0].unitPrice, 16.5);
    assert.equal(updated[0].quantity, 3);
    assert.equal(updated[1].quantity, 1);
    assert.equal(lines[0].quantity, 1);
    assert.equal(cartTotal(updated), 64);
  });

  it('rejects nonpositive additions without changing the cart', () => {
    const lines = addCartLine([], menuItemsMock[0], 1, [], 0, 1);
    assert.strictEqual(addCartLine(lines, menuItemsMock[0], 1, [], 0, 0), lines);
    assert.strictEqual(addCartLine(lines, menuItemsMock[0], 1, [], 0, -1), lines);
  });
});
