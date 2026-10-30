import { ApiError, requestJson } from '../api/client.ts';
import type { OperatorCourier, OperatorCustomer, OperatorOrder, OperatorPage, OperatorRestaurant, OperatorSection, OperatorSectionName, OperatorSummary } from '../types/operator.ts';

type Row = Record<string, unknown>;
const row = (v: unknown): v is Row => typeof v === 'object' && v !== null;
const number = (v: unknown): v is number => typeof v === 'number' && Number.isFinite(v);
const nullableNumber = (v: unknown) => v === null || number(v);
const nullableText = (v: unknown) => v === null || typeof v === 'string';
const contact = (v: Row) => number(v.id) && typeof v.name === 'string' && typeof v.email === 'string' && nullableText(v.phone);
const location = (v: Row) => nullableNumber(v.latitude) && nullableNumber(v.longitude);
const orderStatuses = ['placed', 'accepted', 'preparing', 'ready', 'picked_up', 'delivered', 'cancelled', 'rejected', 'refunded'];

function restaurant(v: unknown): v is OperatorRestaurant {
  return row(v) && number(v.id) && typeof v.name === 'string' && typeof v.status === 'string' && ['open', 'paused', 'closed'].includes(v.status)
    && location(v) && nullableText(v.address) && nullableText(v.phone);
}
function customer(v: unknown): v is OperatorCustomer {
  return row(v) && contact(v) && location(v) && nullableNumber(v.tdCashBalance);
}
function courier(v: unknown): v is OperatorCourier {
  return row(v) && contact(v) && location(v) && nullableText(v.status);
}
function order(v: unknown): v is OperatorOrder {
  return row(v) && number(v.id) && nullableNumber(v.customerId) && nullableNumber(v.restaurantId)
    && nullableNumber(v.courierId) && typeof v.status === 'string' && orderStatuses.includes(v.status) && nullableText(v.dateOrdered)
    && nullableText(v.acceptedAt) && nullableText(v.rejectionReason) && number(v.totalPrice);
}

async function page<T>(kind: OperatorSectionName, index: number, token: string, guard: (v: unknown) => v is T): Promise<OperatorPage<T>> {
  const data = await requestJson<unknown>('GET', `/api/operator/${kind}?page=${index}&size=50`, undefined, token);
  if (!row(data) || !Array.isArray(data.items) || !data.items.every(guard)
    || !Number.isInteger(data.total) || (data.total as number) < data.items.length
    || data.page !== index || data.size !== 50 || data.items.length > 50) {
    throw new ApiError(502, 'The server returned invalid operator dashboard data');
  }
  return data as unknown as OperatorPage<T>;
}

export async function fetchOperatorSummary(token: string): Promise<OperatorSummary> {
  const data = await requestJson<unknown>('GET', '/api/operator/summary', undefined, token);
  const keys = ['restaurantCount', 'orderCount', 'customerCount', 'courierCount', 'availableCourierCount'];
  if (!row(data) || !keys.every((key) => Number.isInteger(data[key]) && (data[key] as number) >= 0)) {
    throw new ApiError(502, 'The server returned invalid operator summary counts');
  }
  return data as unknown as OperatorSummary;
}

export async function fetchOperatorSection(kind: OperatorSectionName, index: number, token: string): Promise<OperatorSection> {
  if (!Number.isInteger(index) || index < 0) throw new Error('Invalid page');
  switch (kind) {
    case 'restaurants': return { kind, data: await page(kind, index, token, restaurant) };
    case 'orders': return { kind, data: await page(kind, index, token, order) };
    case 'customers': return { kind, data: await page(kind, index, token, customer) };
    case 'couriers': return { kind, data: await page(kind, index, token, courier) };
  }
}

export function appendOperatorSection(current: OperatorSection | null, next: OperatorSection): OperatorSection {
  if (!current || current.kind !== next.kind || next.data.page !== current.data.page + 1) return next;
  const ids = new Set(current.data.items.map((item) => item.id));
  return { ...next, data: { ...next.data, items: [...current.data.items, ...next.data.items.filter((item) => !ids.has(item.id))] } } as OperatorSection;
}
