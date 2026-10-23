import type { AuthSession, RegistrationInput, Role, SignInInput } from '../types/auth.ts';
import { postJson } from './client.ts';
import { demoRestaurantLogin, isDemoRestaurantToken } from '../mocks/restaurantAccount.ts';

type BackendRole = 'CUSTOMER' | 'COURIER' | 'STAFF' | 'OPERATOR';

/** Response body of POST /api/auth/register. */
export type RegisterResponse = {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: BackendRole;
};

/** Response body of POST /api/auth/login. */
export type LoginResponse = RegisterResponse & {
  token: string;
  expiresAt: string;
};

// Restaurant accounts are stored with the backend's 'staff' role
const TO_BACKEND_ROLE: Record<Role, BackendRole> = {
  customer: 'CUSTOMER',
  restaurant: 'STAFF',
  courier: 'COURIER',
  Operator: 'OPERATOR',
};

const FROM_BACKEND_ROLE: Record<BackendRole, Role> = {
  CUSTOMER: 'customer',
  STAFF: 'restaurant',
  COURIER: 'courier',
  OPERATOR: 'Operator',
};

const ROLE_LABELS: Record<Role, string> = {
  customer: 'customer',
  restaurant: 'restaurant',
  courier: 'courier',
  Operator: 'operator',
};

/** The backend does not store addresses yet, so the address is not sent. */
export function toRegisterRequest(input: RegistrationInput) {
  return {
    name: input.role === 'restaurant' ? input.restaurantName : input.name,
    email: input.email,
    password: input.password,
    phone: input.phoneNumber,
    role: TO_BACKEND_ROLE[input.role],
  };
}

export async function register(input: RegistrationInput): Promise<void> {
  await postJson<RegisterResponse>('/api/auth/register', toRegisterRequest(input));
}

export async function logout(token: string): Promise<void> {
  if (isDemoRestaurantToken(token)) return;
  await postJson<void>('/api/auth/logout', {}, token);
}

/** Signs in, then rejects the session if the account does not belong to the selected role. */
export async function login(input: SignInInput): Promise<AuthSession> {
  if (input.role === 'restaurant') {
    const demoSession = demoRestaurantLogin(input.email, input.password);
    if (demoSession) return demoSession;
  }
  const response = await postJson<LoginResponse>('/api/auth/login', {
    email: input.email,
    password: input.password,
  });
  const role = FROM_BACKEND_ROLE[response.role];

  if (role !== input.role) {
    await logout(response.token).catch(() => {});
    throw new Error(`This is not a ${ROLE_LABELS[input.role]} account`);
  }
  return {
    token: response.token,
    user: { id: response.id, role, name: response.name, email: response.email, phoneNumber: response.phone },
  };
}
