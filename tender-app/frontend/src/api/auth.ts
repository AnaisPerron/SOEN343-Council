import type { AuthSession, RegistrationInput, Role, SignInInput } from '../types/auth.ts';
import { ApiError, postJson, requestJson } from './client.ts';

type BackendRole = 'CUSTOMER' | 'COURIER' | 'STAFF' | 'OPERATOR';

/** Response body of POST /api/auth/register. */
export type RegisterResponse = {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: BackendRole;
  restaurantId?: number | null;
  address?: string | null;
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
    ...(input.role === 'restaurant' ? { address: [input.address.addressLine, input.address.city, input.address.country].join(', ') } : {}),
  };
}

export async function register(input: RegistrationInput): Promise<void> {
  await postJson<RegisterResponse>('/api/auth/register', toRegisterRequest(input));
}

export async function logout(token: string): Promise<void> {
  await postJson<void>('/api/auth/logout', {}, token);
}

export async function updateProfile(token: string, name: string, phone: string, address?: string): Promise<RegisterResponse> {
  return requestJson<RegisterResponse>('PATCH', '/api/profile', { name, phone, ...(address === undefined ? {} : { address }) }, token);
}

function toSession(response: LoginResponse): AuthSession {
  return {
    token: response.token,
    user: {
      id: response.id,
      role: FROM_BACKEND_ROLE[response.role],
      name: response.name,
      email: response.email,
      phoneNumber: response.phone,
      ...(response.restaurantId == null ? {} : { restaurantId: response.restaurantId }),
      ...(response.address == null ? {} : { address: response.address }),
    },
  };
}

export async function restoreSession(token: string): Promise<AuthSession> {
  try {
    const response = await requestJson<LoginResponse>('GET', '/api/auth/session', undefined, token);
    return toSession(response);
  } catch (error) {
    // Older running backends do not expose /api/auth/session yet, but /api/profile
    // still validates the token and returns the account attached to it.
    if (!(error instanceof ApiError) || error.status !== 404) throw error;
    const profile = await requestJson<RegisterResponse>('GET', '/api/profile', undefined, token);
    return toSession({ ...profile, token, expiresAt: '' });
  }
}

/** Signs in, then rejects the session if the account does not belong to the selected role. */
export async function login(input: SignInInput): Promise<AuthSession> {
  const response = await postJson<LoginResponse>('/api/auth/login', {
    email: input.email,
    password: input.password,
  });
  const role = FROM_BACKEND_ROLE[response.role];

  if (role !== input.role) {
    await logout(response.token).catch(() => {});
    throw new Error(`This is not a ${ROLE_LABELS[input.role]} account`);
  }
  return toSession(response);
}
