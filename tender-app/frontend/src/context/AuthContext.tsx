import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';

import { login, logout, register } from '@/api/auth';
import type { AuthSession, AuthUser, RegistrationInput, Role, SignInInput } from '@/types/auth';

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

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session?.user ?? null,
      signIn: async (input) => setSession(await login(input)),
      signUp: register,
      // Sign out locally right away; the backend session expires on its own if this request fails
      signOut: () => {
        if (session) logout(session.token).catch(() => {});
        setSession(null);
      },
    }),
    [session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}
