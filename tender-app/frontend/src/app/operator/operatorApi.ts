import { API_URL, ApiError } from '../../api/client.ts';
import { updateProfile, type RegisterResponse } from '../../api/auth.ts';
import type { RestaurantStatus } from '../../types/restaurant.ts';

export interface ProfileUpdateRequest {
  name: string;
  phone: string;
}

export type ProfileResponse = RegisterResponse;

// Operator action paths are contracts for endpoints that still need implementation.
export const operatorPaths = {
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

/** UC-ORD-06: an operator sets a restaurant to open, paused or closed. */
export async function changeRestaurantStatus(restaurantId: number, status: RestaurantStatus, token: string): Promise<void> {
  await putJson<void>(operatorPaths.restaurantStatus(restaurantId), { status }, token);
}

/** Uses the existing PATCH /api/profile contract. Password updates are not supported. */
export function updateOperatorProfile(request: ProfileUpdateRequest, token: string): Promise<ProfileResponse> {
  return updateProfile(token, request.name, request.phone);
}
