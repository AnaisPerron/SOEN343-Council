import type { Role } from '../types/auth';

export type AccountMenuCommand = 'profile' | 'history' | 'signOut';

export function getAccountMenuCommands(role: Role): AccountMenuCommand[] {
  return role === 'customer' ? ['profile', 'history', 'signOut'] : ['profile', 'signOut'];
}
