import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { selectedRole } from '../../app/auth/selectedRole.ts';

describe('role carried between sign in and registration', () => {
  it('keeps the selected restaurant role', () => {
    assert.equal(selectedRole('restaurant'), 'restaurant');
  });

  it('keeps the selected courier role', () => {
    assert.equal(selectedRole('courier'), 'courier');
  });

  it('defaults only when the route has no valid role', () => {
    assert.equal(selectedRole(undefined), 'customer');
    assert.equal(selectedRole('unknown'), 'customer');
  });
});
