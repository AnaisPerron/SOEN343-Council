import { requestJson } from '../../api/client.ts';
import { updateProfile, type RegisterResponse } from '../../api/auth.ts';
import type { RestaurantStatus } from '../../types/restaurant.ts';

export interface ProfileUpdateRequest {
  name: string;
  phone: string;
}

export type ProfileResponse = RegisterResponse;

export const operatorPaths = {
  restaurantStatus: (restaurantId: number) => `/api/operator/restaurants/${restaurantId}/status`,
  profile: '/api/profile',
};

export async function changeRestaurantStatus(restaurantId: number, status: RestaurantStatus, token: string): Promise<void> {
  await requestJson<void>('PUT', operatorPaths.restaurantStatus(restaurantId), { status }, token);
}

export async function resetDemoSeed(token: string): Promise<void> {
  await requestJson<void>('POST', '/api/operator/reset-seed', undefined, token);
}

export function updateOperatorProfile(request: ProfileUpdateRequest, token: string): Promise<ProfileResponse> {
  return updateProfile(token, request.name, request.phone);
}
