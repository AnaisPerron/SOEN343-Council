// I, Anais Perron, certify that these tests were gener
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { createMockAuthStore } from '../../auth/mockAuthStore.ts';
import type { RegistrationInput, SignInInput } from '../../types/auth.ts';

const address = { country: 'Canada', city: 'Montreal', addressLine: '123 Main St' };

describe('mock auth store', () => {
  it('registers a customer and signs in with their email and password', () => {
    const store = createMockAuthStore();
    store.register({
      role: 'customer',
      name: 'Alex Morgan',
      email: '  Alex@Example.com ',
      password: 'customer-pass',
      phoneNumber: '5145550123',
      address,
    });

    assert.deepEqual(store.signIn({ role: 'customer', email: 'alex@example.com', password: 'customer-pass' }), {
      role: 'customer',
      id: 1,
      name: 'Alex Morgan',
      email: 'alex@example.com',
      phoneNumber: '5145550123',
      address,
    });
  });

  it('keeps restaurantName as the restaurant profile name', () => {
    const store = createMockAuthStore();
    store.register({
      role: 'restaurant',
      restaurantName: 'Maple & Main',
      email: 'hello@maple.example',
      password: 'restaurant-pass',
      phoneNumber: '5145550199',
      address,
    });

    assert.deepEqual(store.signIn({ role: 'restaurant', email: 'hello@maple.example', password: 'restaurant-pass' }), {
      role: 'restaurant',
      id: 1,
      restaurantName: 'Maple & Main',
      email: 'hello@maple.example',
      phoneNumber: '5145550199',
      address,
    });
  });

  it('allows an email on only one account, regardless of role or letter case', () => {
    const store = createMockAuthStore();
    store.register({
      role: 'customer', name: 'Alex', email: 'alex@example.com',
      password: 'customer-pass', phoneNumber: '5145550123', address,
    });

    assert.throws(() => store.register({
      role: 'restaurant', restaurantName: 'Alex Kitchen', email: ' ALEX@EXAMPLE.COM ',
      password: 'restaurant-pass', phoneNumber: '5145550199', address,
    }), /account with this email already exists/);
  });

  it('rejects an unknown account, an incorrect password, and the wrong profile role', () => {
    const store = createMockAuthStore();
    store.register({
      role: 'customer', name: 'Alex', email: 'alex@example.com',
      password: 'customer-pass', phoneNumber: '5145550123', address,
    });

    assert.throws(() => store.signIn({ role: 'customer', email: 'missing@example.com', password: 'customer-pass' }), /Invalid email or password/);
    assert.throws(() => store.signIn({ role: 'customer', email: 'alex@example.com', password: 'wrong-pass' }), /Invalid email or password/);
    assert.throws(() => store.signIn({ role: 'restaurant', email: 'alex@example.com', password: 'customer-pass' }), /Invalid email or password/);
  });

  it('uses ID and password for operator sign in and rejects invalid IDs', () => {
    const store = createMockAuthStore();

    assert.deepEqual(store.signIn({ role: 'Operator', id: 42, password: 'operator-pass' }), { role: 'Operator', id: 42 });
    assert.throws(() => store.signIn({ role: 'Operator', id: 0, password: 'operator-pass' }), /Invalid operator ID or password/);
    assert.throws(() => store.signIn({ role: 'Operator', id: 42, password: '' }), /Invalid operator ID or password/);
  });

  it('does not allow operator or courier account creation or courier sign in', () => {
    const store = createMockAuthStore();

    assert.throws(() => store.register({ role: 'Operator', id: 42, password: 'operator-pass' } as unknown as RegistrationInput), /Registration is unavailable/);
    assert.throws(() => store.register({ role: 'courier', name: 'Taylor', email: 'taylor@example.com', password: 'courier-pass', phoneNumber: '5145550177' } as unknown as RegistrationInput), /Registration is unavailable/);
    assert.throws(() => store.signIn({ role: 'courier', email: 'taylor@example.com', password: 'courier-pass' } as SignInInput), /Sign in is unavailable/);
  });
});
