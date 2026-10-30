import { API_URL, ApiError, postJson } from '../../api/client.ts';
import type { RestaurantStatus } from '../../types/restaurant.ts';

export interface ProfileUpdateRequest {
  phone?: string;
  password?: string;
}

export interface ProfileResponse {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: string;
}

// backend paths
export const operatorPaths = {
  cancelOrder: (orderId: number) => `/api/operator/orders/${orderId}/cancel`,
  restaurantStatus: (restaurantId: number) => `/api/operator/restaurants/${restaurantId}/status`,
  profile: '/api/profile',
};

async function putJson<T>(path: string, body: unknown, token: string): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, `Cannot reach the server at ${API_URL}`);
  }
  if (!response.ok) {
    const data = (await response.json().catch(() => null)) ?? {};
    const fieldErrors: Record<string, string> = data.fieldErrors ?? {};
    const message = Object.values(fieldErrors)[0] ?? data.message ?? `Request failed (${response.status})`;
    throw new ApiError(response.status, message, fieldErrors);
  }
  return (await response.json().catch(() => null)) as T;
}

/** UC-ORD-05: an operator cancels an active order. */
export async function cancelOrder(orderId: number, token: string): Promise<void> {
  await postJson<void>(operatorPaths.cancelOrder(orderId), {}, token);
}

/** UC-ORD-06: an operator sets a restaurant to open, paused or closed. */
export async function changeRestaurantStatus(restaurantId: number, status: RestaurantStatus, token: string): Promise<void> {
  await putJson<void>(operatorPaths.restaurantStatus(restaurantId), { status }, token);
}

/** UC-REG-03: the operator updates their phone number and/or password. */
export function updateOperatorProfile(request: ProfileUpdateRequest, token: string): Promise<ProfileResponse> {
  return putJson<ProfileResponse>(operatorPaths.profile, request, token);
}