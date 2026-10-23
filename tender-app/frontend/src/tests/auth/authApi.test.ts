import assert from 'node:assert/strict';
import { afterEach, describe, it, mock } from 'node:test';

import { login, logout, register, toRegisterRequest } from '../../api/auth.ts';
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

  it('registers restaurants as staff using the restaurant name', () => {
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
  };

  it('posts credentials and maps the backend account to the app user', async () => {
    const fetchMock = mockFetch(200, loginResponse);
    const session = await login({ role: 'restaurant', email: 'hello@maple.example', password: 'restaurant-pass' });

    const [url, init] = fetchMock.mock.calls[0].arguments as [string, RequestInit];
    assert.equal(url, `${API_URL}/api/auth/login`);
    assert.deepEqual(JSON.parse(init.body as string), { email: 'hello@maple.example', password: 'restaurant-pass' });
    assert.deepEqual(session, {
      token: 'abc123',
      user: { id: 1, role: 'restaurant', name: 'Maple & Main', email: 'hello@maple.example', phoneNumber: '5145550199' },
    });
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
});
