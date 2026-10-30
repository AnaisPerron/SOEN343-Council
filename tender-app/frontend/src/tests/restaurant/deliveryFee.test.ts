import assert from 'node:assert/strict';
import { test } from 'node:test';
import { parseDeliveryFee } from '../../services/restaurant/deliveryFee.ts';

test('delivery fees accept decimal amounts and reject invalid values', () => {
  assert.equal(parseDeliveryFee('1.'), 1);
  assert.equal(parseDeliveryFee('0.50'), 0.5);
  assert.equal(parseDeliveryFee('12,75'), 12.75);
  assert.equal(parseDeliveryFee(''), 0);
  assert.equal(parseDeliveryFee('1.234'), null);
  assert.equal(parseDeliveryFee('-1'), null);
  assert.equal(parseDeliveryFee('abc'), null);
});
