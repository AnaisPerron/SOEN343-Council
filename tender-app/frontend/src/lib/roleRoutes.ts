import type { Role } from '../types/auth';

export function homeRoute(role?: Role) {
  if (role === 'Operator') return '/operator/dashboard' as const;
  if (role === 'customer') return '/customer/home/home' as const;
  if (role === 'restaurant') return '/restaurant' as const;
  if (role === 'courier') return '/courier' as const;
  if (role) return '/account' as const;
  return '/auth/login' as const;
}
