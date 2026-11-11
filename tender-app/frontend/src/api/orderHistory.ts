import { requestJson } from './client.ts';

export type LedgerHistoryEntry = {
  orderId: number;
  status: string;
  placedAt: string | null;
  pickedUpAt: string | null;
  deliveredAt: string | null;
  totalCharge: number;
};

export const fetchLedgerHistory = (token: string) =>
  requestJson<LedgerHistoryEntry[]>('GET', '/api/pricing/history', undefined, token);
