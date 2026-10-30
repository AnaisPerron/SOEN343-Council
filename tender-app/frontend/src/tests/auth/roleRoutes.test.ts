import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { homeRoute } from '../../lib/roleRoutes.ts';

describe('account destinations', () => {
  it('routes operators to the dashboard', () => {
    assert.equal(homeRoute('Operator'), '/operator/dashboard');
  });

  it('keeps customer and other account destinations', () => {
    assert.equal(homeRoute('customer'), '/customer/home/home');
    assert.equal(homeRoute('restaurant'), '/account');
    assert.equal(homeRoute('courier'), '/account');
  });

  it('routes signed out users to login', () => {
    assert.equal(homeRoute(undefined), '/auth/login');
  });
});
