import type { RegistrationInput, Role } from '../types/auth.ts';
import { postJson } from './client.ts';

type BackendRole = 'CUSTOMER' | 'COURIER' | 'STAFF' | 'OPERATOR';

/** Response body of POST /api/auth/register. */
export type RegisterResponse = {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: BackendRole;
};

// Restaurant accounts are stored with the backend's 'staff' role
const TO_BACKEND_ROLE: Record<Role, BackendRole> = {
  customer: 'CUSTOMER',
  restaurant: 'STAFF',
  courier: 'COURIER',
  Operator: 'OPERATOR',
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
