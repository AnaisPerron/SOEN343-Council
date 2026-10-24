import type { AuthSession } from '../types/auth';

const DEMO_PASSWORD = 'DemoPass123!';
const TOKEN_PREFIX = 'mock-restaurant:';
const DEMO_RESTAURANTS = [
  { email: 'greenbowl@demo.test', id: -2, restaurantId: 2, name: 'Green Bowl Kitchen' },
  { email: 'littleitaly@demo.test', id: -4, restaurantId: 4, name: 'Little Italy Pizza' },
] as const;

export function demoRestaurantForToken(token: string) {
  if (!token.startsWith(TOKEN_PREFIX)) return null;
  return DEMO_RESTAURANTS.find((account) => account.email === token.slice(TOKEN_PREFIX.length)) ?? null;
}

export function demoRestaurantLogin(email: string, password: string): AuthSession | null {
  const normalizedEmail = email.trim().toLowerCase();
  const account = DEMO_RESTAURANTS.find((entry) => entry.email === normalizedEmail);
  if (!account || password !== DEMO_PASSWORD) return null;

  return {
    token: `${TOKEN_PREFIX}${normalizedEmail}`,
    user: {
      id: account.id,
      role: 'restaurant',
      name: account.name,
      email: normalizedEmail,
      phoneNumber: '',
    },
  };
}

export function isDemoRestaurantToken(token: string): boolean {
  return demoRestaurantForToken(token) !== null;
}
