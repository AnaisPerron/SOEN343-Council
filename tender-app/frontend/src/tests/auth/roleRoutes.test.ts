import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { homeRoute } from '../../lib/roleRoutes.ts';
import { getAccountMenuCommands } from '../../lib/roleCommands.ts';

describe('account destinations', () => {
  it('routes operators to the dashboard', () => {
    assert.equal(homeRoute('Operator'), '/operator/dashboard');
  });

  describe('role-scoped account commands', () => {
    it('shows only customer commands to customers', () => {
      assert.deepEqual(getAccountMenuCommands('customer'), ['profile', 'history', 'signOut']);
    });

    it('shows only shared account commands to restaurants, couriers, and operators', () => {
      for (const role of ['restaurant', 'courier', 'Operator'] as const) {
        assert.deepEqual(getAccountMenuCommands(role), ['profile', 'signOut']);
      }
    });
  });

  it('keeps customer and other account destinations', () => {
    assert.equal(homeRoute('customer'), '/customer/home/home');
    assert.equal(homeRoute('restaurant'), '/restaurant');
    assert.equal(homeRoute('courier'), '/courier');
  });

  it('routes signed out users to login', () => {
    assert.equal(homeRoute(undefined), '/auth/login');
  });
});
