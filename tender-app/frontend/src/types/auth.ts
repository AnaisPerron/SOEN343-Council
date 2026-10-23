export type ProfileRole = 'customer' | 'restaurant' | 'courier';
export type Role = ProfileRole | 'Operator';

export interface Address {
  country: string;
  city: string;
  addressLine: string;
}

export interface Profile {
  id: number;
  name: string;
  email: string;
  password: string;
  phoneNumber: string;
}

export interface CustomerProfile extends Profile {
  role: 'customer';
  address: Address;
}

export interface RestaurantProfile extends Profile {
  role: 'restaurant';
  address: Address;
}

export interface CourierProfile extends Profile {
  role: 'courier';
}

export interface OperatorAccount {
  role: 'Operator';
  id: number;
  password: string;
}

export type Account = CustomerProfile | RestaurantProfile | CourierProfile | OperatorAccount;
export type RegistrationInput = Omit<CustomerProfile, 'id'> | Omit<RestaurantProfile, 'id'>;
export type ProfileSignIn = {
  role: 'customer' | 'restaurant';
  email: string;
  password: string;
};
export type SignInInput = ProfileSignIn | OperatorAccount;

export type AuthUser =
  | Omit<OperatorAccount, 'password'>
  | Omit<CustomerProfile, 'password'>
  | Omit<RestaurantProfile, 'password'>
  | Omit<CourierProfile, 'password'>;
