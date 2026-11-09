import assert from 'node:assert/strict';
import { afterEach, describe, it, mock } from 'node:test';

import { API_URL } from '../../api/client.ts';
import { fetchMapData, watchMapData } from '../../api/dashMap.ts';
import { createProjection, DEFAULT_MAP_BOUNDS, fitMapSize, isMapEmpty, mapTiles, markerStateLabel, normalizeMapData, toMarkers } from '../../lib/cityMap.ts';
import type { MapCourier, MapData } from '../../types/dash.ts';

// Seeded Montreal demo data
const data: MapData = {
  bounds: { minLatitude: 45.501, minLongitude: -73.6132, maxLatitude: 45.5352, maxLongitude: -73.561 },
  restaurants: [
    { id: 1, name: 'Maple & Main', status: 'open', latitude: 45.51, longitude: -73.57 },
    { id: 4, name: 'Little Italy Pizza', status: 'open', latitude: 45.5352, longitude: -73.6132 },
    { id: 3, name: 'Cafe Nord', status: 'closed', latitude: 45.501, longitude: -73.584 },
  ],
  couriers: [{ courierId: 6, name: 'Sam Courier', status: 'idle', latitude: 45.509, longitude: -73.561 }],
  customers: [],
};

const inside = (value: number) => value > 0 && value < 1;

describe('city map scaling', () => {
  it('keeps every marker inside the map with a margin', () => {
    for (const marker of toMarkers(data)) {
      assert.ok(inside(marker.x) && inside(marker.y), `${marker.name} is off the map`);
    }
  });

  it('puts north at the top and east on the right', () => {
    const { project } = createProjection(data.bounds!);
    const north = project(45.53, -73.58);
    const south = project(45.505, -73.58);
    const east = project(45.51, -73.565);
    const west = project(45.51, -73.61);
    assert.ok(north.y < south.y);
    assert.ok(east.x > west.x);
  });

  it('keeps distances true in both directions', () => {
    // At 45° a degree of longitude covers about cos(45°) of a degree of latitude
    const bounds = { minLatitude: 44.9, minLongitude: -73.2, maxLatitude: 45.1, maxLongitude: -72.8 };
    const { aspect, project } = createProjection(bounds);
    const a = project(45, -73.1);
    const b = project(45, -72.9);
    const c = project(45.1, -73.1);
    const eastWest = b.x - a.x;
    const northSouth = (a.y - c.y) * aspect;
    assert.ok(Math.abs(eastWest / northSouth - 0.2 * Math.cos(Math.PI / 4) / 0.1) < 0.01);
  });

  it('centres a single location instead of dividing by zero', () => {
    const { aspect, project } = createProjection({ minLatitude: 45.5, minLongitude: -73.5, maxLatitude: 45.5, maxLongitude: -73.5 });
    assert.ok(Number.isFinite(aspect) && aspect > 0);
    assert.deepEqual(project(45.5, -73.5), { x: 0.5, y: 0.5 });
  });

  it('never draws a thin strip when every location is on one street', () => {
    const { aspect } = createProjection({ minLatitude: 45.5, minLongitude: -73.7, maxLatitude: 45.5, maxLongitude: -73.5 });
    assert.ok(aspect >= 0.4);
  });

  it('fits the map in the available space without stretching it', () => {
    assert.deepEqual(fitMapSize(0.5, 800, 480), { width: 800, height: 400 });
    assert.deepEqual(fitMapSize(1.2, 800, 480), { width: 400, height: 480 });
  });
});

describe('city map markers', () => {
  it('gives restaurants and couriers their own marker kind with name and state', () => {
    const markers = toMarkers(data);
    assert.deepEqual(markers.map((m) => [m.kind, m.name, m.state]), [
      ['restaurant', 'Maple & Main', 'open'],
      ['restaurant', 'Little Italy Pizza', 'open'],
      ['restaurant', 'Cafe Nord', 'closed'],
      ['courier', 'Sam Courier', 'idle'],
    ]);
    assert.equal(new Set(markers.map((m) => m.key)).size, markers.length);
  });

  it('shows nothing when no location is known', () => {
    const empty = { bounds: DEFAULT_MAP_BOUNDS, restaurants: [], couriers: [], customers: [] };
    assert.deepEqual(toMarkers(empty), []);
    assert.equal(isMapEmpty(empty), true);
    assert.equal(isMapEmpty(data), false);
  });

  it('labels the kind and state', () => {
    assert.equal(markerStateLabel({ kind: 'restaurant', state: 'paused' }), 'Restaurant · Paused');
    assert.equal(markerStateLabel({ kind: 'courier', state: 'carrying' }), 'Courier · Carrying');
  });
});

describe('empty city map', () => {
  it('uses the default area when the response has no bounds or lists', () => {
    for (const response of [null, undefined, {}, { bounds: null as never, restaurants: [] }]) {
      assert.deepEqual(normalizeMapData(response), { bounds: DEFAULT_MAP_BOUNDS, restaurants: [], couriers: [], customers: [] });
    }
  });

  it('still draws a map of normal shape around the default area', () => {
    const { aspect, project } = createProjection(DEFAULT_MAP_BOUNDS);
    assert.ok(aspect >= 0.4 && aspect <= 1.5);
    const centre = project(45.515, -73.585);
    assert.ok(Math.abs(centre.x - 0.5) < 1e-9 && Math.abs(centre.y - 0.5) < 1e-9);
  });
});

describe('city map API', () => {
  afterEach(() => mock.restoreAll());

  it('reads the map with the session token', async () => {
    const fetchMock = mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify(data), { status: 200 }));

    assert.deepEqual(await fetchMapData('session-token'), data);

    const [url, request] = fetchMock.mock.calls[0].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/map`);
    assert.equal(request.method, 'GET');
    assert.equal((request.headers as Record<string, string>).Authorization, 'Bearer session-token');
  });

  it('turns an empty response into an empty map instead of an error', async () => {
    mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify({ restaurants: [], couriers: [] }), { status: 200 }));

    assert.deepEqual(await fetchMapData('session-token'), { bounds: DEFAULT_MAP_BOUNDS, restaurants: [], couriers: [], customers: [] });
  });
});

describe('keeping the map in sync', () => {
  afterEach(() => {
    mock.timers.reset();
    mock.restoreAll();
  });

  // Lets the mocked fetch and the promise chain behind it settle
  const settle = () => new Promise<void>((resolve) => setImmediate(resolve));
  const withCourier = (status: MapCourier['status'] | null): MapData => ({
    ...data,
    couriers: status ? [{ courierId: 6, name: 'Sam Courier', status, latitude: 45.509, longitude: -73.561 }] : [],
  });

  it('reloads on every tick so duty, dispatch, pickup, delivery and reset changes appear', async () => {
    mock.timers.enable({ apis: ['setInterval'] });
    const states = [withCourier(null), withCourier('idle'), withCourier('assigned'), withCourier('carrying'), withCourier('idle'),
      { bounds: DEFAULT_MAP_BOUNDS, restaurants: [], couriers: [] }];
    let call = 0;
    mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify(states[call++]), { status: 200 }));
    const seen: (string | null)[] = [];

    const stop = watchMapData('session-token', (next) => seen.push(next.couriers[0]?.status ?? null), assert.fail, 1000);
    await settle();
    for (let tick = 1; tick < states.length; tick++) {
      mock.timers.tick(1000);
      await settle();
    }
    stop();

    assert.deepEqual(seen, [null, 'idle', 'assigned', 'carrying', 'idle', null]);
  });

  it('does not stack requests while one is still loading', async () => {
    mock.timers.enable({ apis: ['setInterval'] });
    let release: () => void = () => {};
    const fetchMock = mock.method(globalThis, 'fetch', () => new Promise<Response>((resolve) => {
      release = () => resolve(new Response(JSON.stringify(data), { status: 200 }));
    }));

    const stop = watchMapData('session-token', () => {}, assert.fail, 1000);
    mock.timers.tick(1000);
    mock.timers.tick(1000);
    assert.equal(fetchMock.mock.callCount(), 1);

    release();
    await settle();
    mock.timers.tick(1000);
    assert.equal(fetchMock.mock.callCount(), 2);
    stop();
  });

  it('stops reloading and ignores late answers once stopped', async () => {
    mock.timers.enable({ apis: ['setInterval'] });
    const fetchMock = mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify(data), { status: 200 }));
    let updates = 0;

    const stop = watchMapData('session-token', () => { updates += 1; }, assert.fail, 1000);
    stop();
    await settle();
    mock.timers.tick(5000);
    await settle();

    assert.equal(fetchMock.mock.callCount(), 1);
    assert.equal(updates, 0);
  });

  it('reports a failed reload so the map can show it, then recovers on the next tick', async () => {
    mock.timers.enable({ apis: ['setInterval'] });
    const responses = [new Response('{}', { status: 500 }), new Response(JSON.stringify(data), { status: 200 })];
    mock.method(globalThis, 'fetch', async () => responses.shift()!);
    const events: string[] = [];

    const stop = watchMapData('session-token', () => events.push('data'), () => events.push('error'), 1000);
    await settle();
    mock.timers.tick(1000);
    await settle();
    stop();

    assert.deepEqual(events, ['error', 'data']);
  });
});

describe('a map that fills the screen', () => {
  for (const aspect of [0.4, 1, 2.5]) {
    it(`takes the screen's shape (${aspect}) and keeps every marker inside`, () => {
      assert.equal(createProjection(data.bounds, aspect).aspect, aspect);
      for (const marker of toMarkers(data, aspect)) {
        assert.ok(inside(marker.x) && inside(marker.y), `${marker.name} is off the map`);
      }
    });
  }

  it('keeps distances true however wide the screen is', () => {
    const [a, b] = data.restaurants;
    const ratio = (aspect: number) => {
      const { project } = createProjection(data.bounds, aspect);
      const p = project(a.latitude, a.longitude);
      const q = project(b.latitude, b.longitude);
      // Screen pixels per degree must be the same across and down for a given map width of 1
      return Math.abs(q.x - p.x) / (Math.abs(q.y - p.y) * aspect);
    };
    assert.ok(Math.abs(ratio(0.5) - ratio(2)) < 1e-9);
  });
});

describe('customer locations', () => {
  const delivery: MapData = {
    ...data,
    restaurants: [data.restaurants[0]],
    customers: [{ customerId: 2, name: 'Cara Customer', latitude: 45.52, longitude: -73.58 }],
  };

  it('places the customer between the restaurant and the courier so the courier draws on top', () => {
    assert.deepEqual(toMarkers(delivery).map((m) => [m.kind, m.name]), [
      ['restaurant', 'Maple & Main'],
      ['customer', 'Cara Customer'],
      ['courier', 'Sam Courier'],
    ]);
  });

  it('labels a customer without a state', () => {
    const customer = toMarkers(delivery).find((m) => m.kind === 'customer')!;
    assert.equal(markerStateLabel(customer), 'Customer');
  });

  it('counts a lone customer as something to show', () => {
    assert.equal(isMapEmpty({ ...delivery, restaurants: [], couriers: [] }), false);
  });
});

describe('mapTiles', () => {
  it('covers the whole map with street tiles', () => {
    const tiles = mapTiles(data.bounds, 520);
    assert.ok(tiles.length > 0);
    assert.ok(tiles.every((t) => t.url.startsWith('https://tile.openstreetmap.org/')));
    assert.ok(Math.min(...tiles.map((t) => t.x)) <= 0);
    assert.ok(Math.min(...tiles.map((t) => t.y)) <= 0);
    assert.ok(Math.max(...tiles.map((t) => t.x + t.width)) >= 1);
    assert.ok(Math.max(...tiles.map((t) => t.y + t.height)) >= 1);
  });

  it('shows each tile at about its natural size', () => {
    const tiles = mapTiles(data.bounds, 520);
    const px = tiles[0].width * 520;
    assert.ok(px > 256 / Math.SQRT2 && px < 256 * Math.SQRT2, `tile is ${px}px wide`);
  });

  it('draws nothing before the map has a width', () => {
    assert.deepEqual(mapTiles(data.bounds, 0), []);
  });
});
