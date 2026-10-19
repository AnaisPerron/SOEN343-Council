import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { restaurantsMock } from '../../mocks/restaurants.mock.ts';
import { searchRestaurants } from './restaurantSearch.ts';

describe('searchRestaurants', () => {
  it('matches a restaurant name without regard to case', () => {
    assert.deepEqual(
      searchRestaurants(restaurantsMock, 'GREEN BOWL'),
      [restaurantsMock[1]],
    );
  });

  it('trims surrounding whitespace from the query', () => {
    assert.deepEqual(searchRestaurants(restaurantsMock, '  maple & main  '), [restaurantsMock[0]]);
  });

  it('returns all partial name matches from the full restaurant list', () => {
    assert.deepEqual(
      searchRestaurants(restaurantsMock, 'e'),
      [restaurantsMock[0], restaurantsMock[1], restaurantsMock[3]],
    );
  });

  it('returns no results for blank or unmatched queries', () => {
    assert.deepEqual(searchRestaurants(restaurantsMock, '   '), []);
    assert.deepEqual(searchRestaurants(restaurantsMock, 'sushi'), []);
  });
});
