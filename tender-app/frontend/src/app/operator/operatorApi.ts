import { requestJson } from '../../api/client.ts';
import { updateProfile, type RegisterResponse } from '../../api/auth.ts';
import type { RestaurantStatus } from '../../types/restaurant.ts';

export interface ProfileUpdateRequest {
  name: string;
  phone: string;
}

export type ProfileResponse = RegisterResponse;

export const operatorPaths = {
  orderRefund: (orderId: number) => `/api/operator/orders/${orderId}/refund`,
  restaurantStatus: (restaurantId: number) => `/api/operator/restaurants/${restaurantId}/status`,
  profile: '/api/profile',
  reset: '/api/operator/reset',
  reassignOrder: (orderId: number) => `/api/operator/orders/${orderId}/reassign`,
};

export async function changeRestaurantStatus(restaurantId: number, status: RestaurantStatus, token: string): Promise<void> {
  await requestJson<void>('PUT', operatorPaths.restaurantStatus(restaurantId), { status }, token);
}

export async function refundOperatorOrder(orderId: number, token: string): Promise<void> {
  await requestJson('POST', `/api/operator/orders/${orderId}/refund`, undefined, token);
}

export function updateOperatorProfile(request: ProfileUpdateRequest, token: string): Promise<ProfileResponse> {
  return updateProfile(token, request.name, request.phone);
}

export async function resetSystem(token: string): Promise<void> {
  await requestJson<void>('POST', operatorPaths.reset, { confirmation: 'RESET' }, token);
}

export async function reassignOrder(orderId: number, expectedCourierId: number, token: string): Promise<void> {
  await requestJson<void>('POST', operatorPaths.reassignOrder(orderId), { expectedCourierId }, token);
}

export async function refundOrder(orderId: number, token: string): Promise<void> {
  await requestJson<void>('POST', operatorPaths.orderRefund(orderId), undefined, token);
}

export const resetDemoSeed = resetSystem;
