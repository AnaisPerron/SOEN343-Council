import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
 
export type Role = 'user' | 'courier' | 'admin';
 
export const ROLE_OPTIONS: { value: Role; label: string }[] = [
  { value: 'user', label: 'User' },
  { value: 'courier', label: 'Courier' },
  { value: 'admin', label: 'Admin' },
];
 
export type AuthUser = {
  email: string;
  role: Role;
};
 
type AuthContextValue = {
  user: AuthUser | null;
  signIn: (email: string, password: string, role: Role) => Promise<void>;
  signUp: (
    name: string,
    email: string,
    phone: string,
    password: string,
    role: Role,
  ) => Promise<void>;
  signOut: () => void;
};
 
const AuthContext = createContext<AuthContextValue | null>(null);
 
const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
 
//backend to add
async function mockLogin(email: string, password: string, role: Role): Promise<AuthUser> {
  await wait(700);
  if (password.length < 6) throw new Error('Invalid email or password');
  return { email, role };
}
 
async function mockRegister(
  _name: string,
  email: string,
  _phone: string,
  _password: string,
  _role: Role,
): Promise<void> {
  await wait(700);
  if (email.toLowerCase() === 'taken@test.com') {
    throw new Error('An account with this email already exists');
  }
}
 
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
 
  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      signIn: async (email, password, role) => setUser(await mockLogin(email, password, role)),
      signUp: async (name, email, phone, password, role) => {
        await mockRegister(name, email, phone, password, role);
      },
      signOut: () => setUser(null),
    }),
    [user],
  );
 
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
 
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
 