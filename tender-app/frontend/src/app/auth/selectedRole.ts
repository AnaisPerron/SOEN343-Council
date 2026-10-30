import type { Role } from '@/types/auth';

const roles: Role[] = ['customer', 'restaurant', 'courier', 'Operator'];

export function selectedRole(value: string | string[] | undefined): Role {
  return typeof value === 'string' && roles.includes(value as Role) ? value as Role : 'customer';
}
