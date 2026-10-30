import { createContext, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';

import { deleteAccount, login, logout, register, restoreSession, updateProfile } from '@/api/auth';
import { ApiError } from '@/api/client';
import type { AuthSession, AuthUser, RegistrationInput, Role, SignInInput } from '@/types/auth';
import { clearSessionToken, readSessionToken, saveSessionToken } from './sessionStorage';

export type { Role } from '@/types/auth';

export const ROLE_OPTIONS: { value: Role; label: string }[] = [
  { value: 'customer', label: 'Customer' },
  { value: 'restaurant', label: 'Restaurant' },
  { value: 'courier', label: 'Courier' },
  { value: 'Operator', label: 'Operator' },
];

type AuthContextValue = {
  user: AuthUser | null;
  token: string | null;
  ready: boolean;
  signIn: (input: SignInInput) => Promise<void>;
  signUp: (input: RegistrationInput) => Promise<void>;
  signOut: () => void;
  deleteAccount: () => Promise<void>;
  saveProfile: (name: string, phoneNumber: string, address?: string) => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [ready, setReady] = useState(false);
  const generation = useRef(0);

  useEffect(() => {
    const currentGeneration = generation.current;
    const hydrate = async () => {
      try {
        const token = await readSessionToken();
        if (token) {
          const restored = await restoreSession(token);
          if (generation.current === currentGeneration) setSession(restored);
        }
      } catch (error) {
        if (generation.current === currentGeneration && error instanceof ApiError && error.status === 401) {
          await clearSessionToken().catch(() => {});
        }
      } finally {
        if (generation.current === currentGeneration) setReady(true);
      }
    };
    hydrate();
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session?.user ?? null,
      token: session?.token ?? null,
      ready,
      signIn: async (input) => {
        const signedIn = await login(input);
        generation.current += 1;
        await saveSessionToken(signedIn.token);
        setSession(signedIn);
        setReady(true);
      },
      signUp: register,
      deleteAccount: async () => {
        if (!session) throw new Error('Please sign in again');
        await deleteAccount(session.token);
        generation.current += 1;
        await clearSessionToken().catch(() => {});
        setSession(null);
        setReady(true);
      },
      saveProfile: async (name, phoneNumber, address) => {
        if (!session) throw new Error('Please sign in again');
        const updated = await updateProfile(session.token, name, phoneNumber, address);
        setSession((current) => current ? {
          ...current,
          user: { ...current.user, name: updated.name, phoneNumber: updated.phone,
            restaurantId: updated.restaurantId ?? current.user.restaurantId,
            address: updated.address ?? current.user.address },
        } : null);
      },
      // Sign out locally right away; the backend session expires on its own if this request fails
      signOut: () => {
        generation.current += 1;
        if (session) logout(session.token).catch(() => {});
        clearSessionToken().catch(() => {});
        setSession(null);
        setReady(true);
      },
    }),
    [session, ready],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}
