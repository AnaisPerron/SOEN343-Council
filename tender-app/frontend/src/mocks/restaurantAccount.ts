import type { AuthSession } from '../types/auth';

const DEMO_PASSWORD = 'DemoPass123!';
const DEMO_EMAILS = ['littleitaly@demo.test', 'littleitaly.manager@demo.test'];
const TOKEN_PREFIX = 'mock-restaurant:';

export function demoRestaurantLogin(email: string, password: string): AuthSession | null {
  const normalizedEmail = email.trim().toLowerCase();
  if (!DEMO_EMAILS.includes(normalizedEmail) || password !== DEMO_PASSWORD) return null;

  return {
    token: `${TOKEN_PREFIX}${normalizedEmail}`,
    user: {
      id: normalizedEmail === DEMO_EMAILS[0] ? -4 : -5,
      role: 'restaurant',
      name: 'Little Italy Pizza',
      email: normalizedEmail,
      phoneNumber: '',
    },
  };
}

export function isDemoRestaurantToken(token: string): boolean {
  return token.startsWith(TOKEN_PREFIX) && DEMO_EMAILS.includes(token.slice(TOKEN_PREFIX.length));
}
