import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';

import type {
  AuthUser,
  CustomerProfile,
  RegistrationInput,
  RestaurantProfile,
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

// Temporary client-side store until the authentication API is available.
const accountsByEmail = new Map<string, CustomerProfile | RestaurantProfile>();
let nextProfileId = 1;

const normalizeEmail = (email: string) => email.trim().toLowerCase();
const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

async function mockRegister(input: RegistrationInput): Promise<void> {
  await wait(400);
  const email = normalizeEmail(input.email);
  if (accountsByEmail.has(email)) {
    throw new Error('An account with this email already exists');
  }

  const profile = { ...input, id: nextProfileId++, email };
  accountsByEmail.set(email, profile);
}

async function mockLogin(input: SignInInput): Promise<AuthUser> {
  await wait(400);
  if (input.role === 'Operator') {
    if (!Number.isSafeInteger(input.id) || input.id < 1 || !input.password) {
      throw new Error('Invalid operator ID or password');
    }
    return { role: 'Operator', id: input.id };
  }

  const account = accountsByEmail.get(normalizeEmail(input.email));
  if (!account || account.role !== input.role || account.password !== input.password) {
    throw new Error('Invalid email or password');
  }
  return {
    role: account.role,
    id: account.id,
    name: account.name,
    email: account.email,
    phoneNumber: account.phoneNumber,
    address: account.address,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      signIn: async (input) => setUser(await mockLogin(input)),
      signUp: mockRegister,
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
