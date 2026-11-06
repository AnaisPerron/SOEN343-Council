import { requestJson } from './client.ts';

export type CourierState = 'offline' | 'idle' | 'assigned' | 'carrying';
export type AssignedOrder = { orderId: number; status: string; restaurantName: string | null; restaurantAddress: string | null; totalPrice: number | null };
export type CourierDuty = { courierId: number; status: CourierState; latitude: number | null; longitude: number | null; assignedOrder: AssignedOrder | null };

const BASE = '/api/courier/duty';

export const fetchCourierDuty = (token: string) => requestJson<CourierDuty>('GET', BASE, undefined, token);
export const goOnDuty = (token: string) => requestJson<CourierDuty>('POST', `${BASE}/on`, undefined, token);
export const goOffDuty = (token: string) => requestJson<CourierDuty>('POST', `${BASE}/off`, undefined, token);
