import assert from 'node:assert/strict';
import { beforeEach, describe, it } from 'node:test';
import { menuItemsMock } from '../../mocks/menus.mock.ts';
import { addCartLine, changeCartLineQuantity } from '../../app/customer/cart/cartStore.ts';
import { loadCustomerCart, saveCustomerCart } from '../../storage/customerCart.ts';

// Simulates browser storage that survives a page refresh.
function installStorage() {
  const data = new Map<string, string>();
  (globalThis as { localStorage?: unknown }).localStorage = {
    getItem: (key: string) => data.get(key) ?? null,
    setItem: (key: string, value: string) => { data.set(key, value); },
    removeItem: (key: string) => { data.delete(key); },
  };
  return data;
}

describe('customer cart persistence', () => {
  let storage: Map<string, string>;
  beforeEach(() => { storage = installStorage(); });

  it('restores the same items and quantities after a refresh', () => {
    let lines = addCartLine([], menuItemsMock[0], 1, [5001], 2, 2);
    lines = addCartLine(lines, menuItemsMock[1], 1, [], 0, 1);
    saveCustomerCart(7, lines);
    assert.deepEqual(loadCustomerCart(7), lines);
  });

  it('keeps removed items removed after a refresh', () => {
    let lines = addCartLine([], menuItemsMock[0], 1, [], 0, 1);
    lines = addCartLine(lines, menuItemsMock[1], 1, [], 0, 3);
    saveCustomerCart(7, lines);
    lines = changeCartLineQuantity(lines, lines[0].key, 0);
    saveCustomerCart(7, lines);
    const restored = loadCustomerCart(7);
    assert.equal(restored.length, 1);
    assert.equal(restored[0].itemId, menuItemsMock[1].id);
    assert.equal(restored[0].quantity, 3);
  });

  it('keeps an empty cart empty after a refresh', () => {
    assert.deepEqual(loadCustomerCart(7), []);
    saveCustomerCart(7, addCartLine([], menuItemsMock[0], 1, [], 0, 1));
    saveCustomerCart(7, []);
    assert.deepEqual(loadCustomerCart(7), []);
    assert.equal(storage.size, 0);
  });

  it('keeps carts separate per customer and ignores corrupt data', () => {
    saveCustomerCart(7, addCartLine([], menuItemsMock[0], 1, [], 0, 1));
    assert.deepEqual(loadCustomerCart(8), []);
    storage.set('tender-customer-cart-v1:9', '{not json');
    assert.deepEqual(loadCustomerCart(9), []);
    storage.set('tender-customer-cart-v1:9', JSON.stringify([{ key: 'x' }]));
    assert.deepEqual(loadCustomerCart(9), []);
  });

  it('does not persist a signed-out cart', () => {
    saveCustomerCart(undefined, addCartLine([], menuItemsMock[0], 1, [], 0, 1));
    assert.equal(storage.size, 0);
  });
});
