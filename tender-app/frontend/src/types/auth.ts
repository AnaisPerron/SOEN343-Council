export type ProfileRole = 'customer' | 'restaurant' | 'courier';
export type Role = ProfileRole | 'Operator';

export interface Address {
  country: string;
  city: string;
  addressLine: string;
}

export interface Profile {
  id: number;
  email: string;
  password: string;
  phoneNumber: string;
}

export interface CustomerProfile extends Profile {
  role: 'customer';
  name: string;
  address: Address;
}

export interface RestaurantProfile extends Profile {
  role: 'restaurant';
  restaurantName: string;
  address: Address;
}

export interface CourierProfile extends Profile {
  role: 'courier';
  name: string;
}

//Design desicion: should it be considered as a profile? then the Profile superclass would have a way more minimalistic aspect
export interface OperatorAccount {
  role: 'Operator';
  id: number;
  password: string;
}

export type Account = CustomerProfile | RestaurantProfile | CourierProfile | OperatorAccount;
export type RegistrationInput = Omit<CustomerProfile, 'id'> | Omit<RestaurantProfile, 'id'>;
export type SignInInput = {
  role: Role;
  email: string;
  password: string;
};

/** The signed-in account as returned by the backend's login endpoint. */
export interface AuthUser {
  id: number;
  role: Role;
  name: string;
  email: string;
  phoneNumber: string;
  restaurantId?: number;
  address?: string;
}

export interface AuthSession {
  user: AuthUser;
  token: string;
}
