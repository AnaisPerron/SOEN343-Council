import assert from 'node:assert/strict';
import { afterEach, describe, it, mock } from 'node:test';

import { login, logout, register, restoreSession, toRegisterRequest, updateProfile } from '../../api/auth.ts';
import { API_URL } from '../../api/client.ts';
import type { RegistrationInput } from '../../types/auth.ts';

const address = { country: 'Canada', city: 'Montreal', addressLine: '123 Main St' };
const customer: RegistrationInput = {
  role: 'customer',
  name: 'Alex',
  email: 'alex@example.com',
  password: 'customer-pass',
  phoneNumber: '5145550123',
  address,
};

function mockFetch(status: number, body: unknown) {
  return mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify(body), { status }));
}

describe('registration api', () => {
  afterEach(() => mock.restoreAll());

  it('maps a customer registration to the backend request shape', () => {
    assert.deepEqual(toRegisterRequest(customer), {
      name: 'Alex',
      email: 'alex@example.com',
      password: 'customer-pass',
      phone: '5145550123',
      role: 'CUSTOMER',
    });
  });

  it('registers a restaurant with its name and address', () => {
    const request = toRegisterRequest({
      role: 'restaurant',
      restaurantName: 'Maple & Main',
      email: 'hello@maple.example',
      password: 'restaurant-pass',
      phoneNumber: '5145550199',
      address,
    });
    assert.equal(request.role, 'STAFF');
    assert.equal(request.name, 'Maple & Main');
    assert.equal(request.address, '123 Main St, Montreal, Canada');
  });

  it('sends where a picked address is so the customer appears on the map', () => {
    const request = toRegisterRequest({ ...customer, address: { ...address, latitude: 45.4972, longitude: -73.579 } });
    assert.equal(request.latitude, 45.4972);
    assert.equal(request.longitude, -73.579);
    assert.equal('address' in request, false);
  });

  it('places a restaurant at its picked address', () => {
    const request = toRegisterRequest({
      role: 'restaurant',
      restaurantName: 'Maple & Main',
      email: 'hello@maple.example',
      password: 'restaurant-pass',
      phoneNumber: '5145550199',
      address: { ...address, latitude: 45.5017, longitude: -73.5689 },
    });
    assert.equal(request.address, '123 Main St, Montreal, Canada');
    assert.equal(request.latitude, 45.5017);
    assert.equal(request.longitude, -73.5689);
  });

  it('registers a courier with their name and no address', () => {
    assert.deepEqual(toRegisterRequest({
      role: 'courier',
      name: 'Nora',
      email: 'nora@example.com',
      password: 'courier-pass',
      phoneNumber: '5145550107',
    }), {
      name: 'Nora',
      email: 'nora@example.com',
      password: 'courier-pass',
      phone: '5145550107',
      role: 'COURIER',
    });
  });

  it('posts registration to the backend', async () => {
    const fetchMock = mockFetch(201, { id: 1, name: 'Alex', email: 'alex@example.com', phone: '5145550123', role: 'CUSTOMER' });
    await register(customer);
    const [url, init] = fetchMock.mock.calls[0].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/auth/register`);
    assert.equal(init.method, 'POST');
    assert.equal(JSON.parse(init.body as string).phone, '5145550123');
  });

  it('surfaces backend error messages', async () => {
    mockFetch(409, { status: 409, message: 'An account with email alex@example.com already exists', fieldErrors: {} });
    await assert.rejects(register(customer), /already exists/);
  });

  it('prefers field validation messages over the generic one', async () => {
    mockFetch(400, { status: 400, message: 'Validation failed', fieldErrors: { phone: 'Phone must be a valid phone number' } });
    await assert.rejects(register(customer), /Phone must be a valid phone number/);
  });

  it('reports when the server is unreachable', async () => {
    mock.method(globalThis, 'fetch', async () => {
      throw new TypeError('fetch failed');
    });
    await assert.rejects(register(customer), /Cannot reach the server/);
  });
});

describe('profile api', () => {
  afterEach(() => mock.restoreAll());

  it('saves edited name and phone with the signed-in token', async () => {
    const response = { id: 1, name: 'Alex Lee', email: 'alex@example.com', phone: '5145550199', role: 'CUSTOMER' };
    const fetchMock = mockFetch(200, response);
    assert.deepEqual(await updateProfile('saved-token', 'Alex Lee', '5145550199'), response);

    const [url, init] = fetchMock.mock.calls[0].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/profile`);
    assert.equal(init.method, 'PATCH');
    assert.equal((init.headers as Record<string, string>).Authorization, 'Bearer saved-token');
    assert.deepEqual(JSON.parse(init.body as string), { name: 'Alex Lee', phone: '5145550199' });
  });

  it('surfaces profile validation errors', async () => {
    mockFetch(400, { fieldErrors: { phone: 'Phone must be a valid phone number' } });
    await assert.rejects(updateProfile('saved-token', 'Alex', 'bad'), /Phone must be a valid phone number/);
  });

  it('sends an edited restaurant address with the profile update', async () => {
    const fetchMock = mockFetch(200, { id: 1, name: 'Maple Bistro', email: 'hello@maple.example',
      phone: '5145550199', role: 'STAFF', restaurantId: 42, address: '99 Rue St' });
    await updateProfile('saved-token', 'Maple Bistro', '5145550199', '99 Rue St');
    const [, init] = fetchMock.mock.calls[0].arguments as [string, RequestInit];
    assert.deepEqual(JSON.parse(init.body as string), {
      name: 'Maple Bistro', phone: '5145550199', address: '99 Rue St',
    });
  });
});

describe('login api', () => {
  afterEach(() => mock.restoreAll());

  const loginResponse = {
    token: 'abc123',
    expiresAt: '2026-01-02T00:00:00Z',
    id: 1,
    name: 'Maple & Main',
    email: 'hello@maple.example',
    phone: '5145550199',
    role: 'STAFF',
    restaurantId: 42,
    address: '123 Main St, Montreal, Canada',
  };

  it('posts credentials and maps the backend account to the app user', async () => {
    const fetchMock = mockFetch(200, loginResponse);
    const session = await login({ role: 'restaurant', email: 'hello@maple.example', password: 'restaurant-pass' });

    const [url, init] = fetchMock.mock.calls[0].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/auth/login`);
    assert.deepEqual(JSON.parse(init.body as string), { email: 'hello@maple.example', password: 'restaurant-pass' });
    assert.deepEqual(session, {
      token: 'abc123',
      user: { id: 1, role: 'restaurant', name: 'Maple & Main', email: 'hello@maple.example',
        phoneNumber: '5145550199', restaurantId: 42, address: '123 Main St, Montreal, Canada' },
    });
  });

  it('maps a predefined operator login and restored session to the operator role', async () => {
    mockFetch(200, {
      token: 'operator-token', expiresAt: '2026-10-30T12:00:00Z',
      id: 8, name: 'Olivia Operator', email: 'operator@demo.test',
      phone: '+15145550108', role: 'OPERATOR',
    });
    const session = await login({ role: 'Operator', email: 'operator@demo.test', password: 'DemoPass123!' });
    assert.equal(session.user.role, 'Operator');
    assert.equal(session.user.name, 'Olivia Operator');
    const restored = await restoreSession(session.token);
    assert.equal(restored.user.role, 'Operator');
    assert.equal(restored.token, session.token);
  });

  it('reports an unreachable backend for demo credentials', async () => {
    const fetchMock = mock.method(globalThis, 'fetch', async () => { throw new TypeError('fetch failed'); });
    for (const email of ['greenbowl@demo.test', 'littleitaly@demo.test']) {
      await assert.rejects(login({ role: 'restaurant', email, password: 'DemoPass123!' }), /Cannot reach the server/);
    }
    assert.equal(fetchMock.mock.callCount(), 2);
  });

  it('does not accept demo credentials when the backend responds', async () => {
    const fetchMock = mockFetch(401, { status: 401, message: 'Invalid email or password' });
    await assert.rejects(
      login({ role: 'restaurant', email: 'greenbowl@demo.test', password: 'DemoPass123!' }),
      /Invalid email or password/,
    );
    assert.equal(fetchMock.mock.callCount(), 1);
  });

  it('rejects an account of a different role and ends its session', async () => {
    const fetchMock = mockFetch(200, loginResponse);
    await assert.rejects(
      login({ role: 'customer', email: 'hello@maple.example', password: 'restaurant-pass' }),
      /not a customer account/,
    );

    const [url, init] = fetchMock.mock.calls[1].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/auth/logout`);
    assert.equal((init.headers as Record<string, string>).Authorization, 'Bearer abc123');
  });

  it('surfaces invalid credentials', async () => {
    mockFetch(401, { status: 401, message: 'Invalid email or password', fieldErrors: {} });
    await assert.rejects(
      login({ role: 'customer', email: 'alex@example.com', password: 'wrong' }),
      /Invalid email or password/,
    );
  });

  it('sends the session token when logging out', async () => {
    const fetchMock = mock.method(globalThis, 'fetch', async () => new Response(null, { status: 204 }));
    await logout('abc123');

    const [url, init] = fetchMock.mock.calls[0].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/auth/logout`);
    assert.equal((init.headers as Record<string, string>).Authorization, 'Bearer abc123');
  });

  it('validates a saved token and restores its account', async () => {
    const fetchMock = mockFetch(200, loginResponse);
    const session = await restoreSession('abc123');
    const [url, init] = fetchMock.mock.calls[0].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/auth/session`);
    assert.equal(init.method, 'GET');
    assert.equal((init.headers as Record<string, string>).Authorization, 'Bearer abc123');
    assert.equal(session.user.role, 'restaurant');
    assert.equal(session.user.restaurantId, 42);
  });

  it('restores through the profile endpoint when the running backend lacks the session endpoint', async () => {
    const calls: { url: string; init: RequestInit }[] = [];
    mock.method(globalThis, 'fetch', async (url: string | URL | Request, init: RequestInit) => {
      calls.push({ url: String(url), init });
      return new Response(JSON.stringify(calls.length === 1 ? {} : {
        id: 1, name: 'Alex', email: 'alex@example.com', phone: '5145550123', role: 'CUSTOMER',
      }), { status: calls.length === 1 ? 404 : 200 });
    });

    const session = await restoreSession('saved-token');
    assert.equal(calls[0].url, `${API_URL}/api/auth/session`);
    assert.equal(calls[1].url, `${API_URL}/api/profile`);
    assert.equal((calls[1].init.headers as Record<string, string>).Authorization, 'Bearer saved-token');
    assert.equal(session.user.role, 'customer');
    assert.equal(session.token, 'saved-token');
  });
});
