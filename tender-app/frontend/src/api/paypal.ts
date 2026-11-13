import { requestJson } from './client';
import type { OrderSubmission, OrderSubmissionResult } from '../app/customer/services/orderApi';

export interface PayPalConfig { enabled: boolean; clientId: string }

export const fetchPayPalConfig = (token: string) =>
  requestJson<PayPalConfig>('GET', '/api/paypal/config', undefined, token);

export const createPayPalOrder = (token: string, orders: OrderSubmission[], returnUrl?: string) =>
  requestJson<{ orderId: string; approvalUrl: string }>('POST', '/api/paypal/orders', { orders, returnUrl }, token);

export const capturePayPalOrder = (token: string, orderId: string) =>
  requestJson<OrderSubmissionResult[]>('POST', `/api/paypal/orders/${encodeURIComponent(orderId)}/capture`, {}, token);
