import { requestJson } from './client.ts';

export type CourierState = 'offline' | 'idle' | 'assigned' | 'carrying';
export type AssignedOrder = { assignmentId: number; assignmentStatus: 'offered' | 'accepted'; assignedAt: string; acceptedAt: string | null; orderId: number; status: string; restaurantName: string | null; restaurantAddress: string | null; deliveryAddress: string | null };
export type CourierDuty = { courierId: number; status: CourierState; latitude: number | null; longitude: number | null; assignedOrder: AssignedOrder | null };

const BASE = '/api/courier/duty';

export const fetchCourierDuty = (token: string) => requestJson<CourierDuty>('GET', BASE, undefined, token);
export const goOnDuty = (token: string) => requestJson<CourierDuty>('POST', `${BASE}/on`, undefined, token);
export const goOffDuty = (token: string) => requestJson<CourierDuty>('POST', `${BASE}/off`, undefined, token);

export const acceptCourierOffer = (assignmentId: number, token: string) =>
  requestJson<AssignedOrder>('POST', `/api/courier/offers/${assignmentId}/accept`, undefined, token);

export const acceptOrder = (token: string, orderId: number) =>
  requestJson<CourierDuty>('POST', `${BASE}/orders/${orderId}/accept`, undefined, token);
export const declineOrder = (token: string, orderId: number) =>
  requestJson<CourierDuty>('POST', `${BASE}/orders/${orderId}/decline`, undefined, token);
export const markOrderPickedUp = (token: string, orderId: number) =>
  requestJson<CourierDuty>('POST', `${BASE}/orders/${orderId}/picked-up`, undefined, token);
export const markOrderDelivered = (token: string, orderId: number) =>
  requestJson<CourierDuty>('POST', `${BASE}/orders/${orderId}/delivered`, undefined, token);
