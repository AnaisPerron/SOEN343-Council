import type {
  AuthUser,
  CustomerProfile,
  RegistrationInput,
  RestaurantProfile,
  SignInInput,
} from '../types/auth.ts';

const normalizeEmail = (email: string) => email.trim().toLowerCase();

/** Temporary in-memory authentication until the backend API is available. */
export function createMockAuthStore() {
  const accountsByEmail = new Map<string, CustomerProfile | RestaurantProfile>();
  let nextProfileId = 1;

  return {
    register(input: RegistrationInput): void {
      if (input.role !== 'customer' && input.role !== 'restaurant') {
        throw new Error('Registration is unavailable for this role');
      }
      const email = normalizeEmail(input.email);
      if (accountsByEmail.has(email)) {
        throw new Error('An account with this email already exists');
      }

      const profile = { ...input, id: nextProfileId++, email };
      accountsByEmail.set(email, profile);
    },

    signIn(input: SignInInput): AuthUser {
      if (input.role === 'Operator') {
        if (!Number.isSafeInteger(input.id) || input.id < 1 || !input.password) {
          throw new Error('Invalid operator ID or password');
        }
        return { role: 'Operator', id: input.id };
      }
      if (input.role !== 'customer' && input.role !== 'restaurant') {
        throw new Error('Sign in is unavailable for this role');
      }

      const account = accountsByEmail.get(normalizeEmail(input.email));
      if (!account || account.role !== input.role || account.password !== input.password) {
        throw new Error('Invalid email or password');
      }
      const shared = {
        id: account.id,
        email: account.email,
        phoneNumber: account.phoneNumber,
        address: account.address,
      };
      if (account.role === 'restaurant') {
        return { ...shared, role: 'restaurant', restaurantName: account.restaurantName };
      }
      return { ...shared, role: 'customer', name: account.name };
    },
  };
}
