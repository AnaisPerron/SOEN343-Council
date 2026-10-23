import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';

import { register } from '@/api/auth';
import { createMockAuthStore } from '@/auth/mockAuthStore';
import type {
  AuthUser,
  RegistrationInput,
  Role,
  SignInInput,
} from '@/types/auth';

export type { Role } from '@/types/auth';

export const ROLE_OPTIONS: { value: Role; label: string }[] = [
  { value: 'customer', label: 'Customer' },
  { value: 'restaurant', label: 'Restaurant' },
  { value: 'courier', label: 'Courier' },
  { value: 'Operator', label: 'Operator' },
];

type AuthContextValue = {
  user: AuthUser | null;
  signIn: (input: SignInInput) => Promise<void>;
  signUp: (input: RegistrationInput) => Promise<void>;
  signOut: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

// Sign in stays mocked until the backend has a login endpoint
const mockAuthStore = createMockAuthStore();
const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

async function mockLogin(input: SignInInput): Promise<AuthUser> {
  await wait(400);
  return mockAuthStore.signIn(input);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      signIn: async (input) => setUser(await mockLogin(input)),
      signUp: register,
      signOut: () => setUser(null),
    }),
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}
