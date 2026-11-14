import { requestJson } from './client';

export interface TenderCashBalance { tenders: number; cadValue: number }
export const fetchTenderCash = (token: string) =>
  requestJson<TenderCashBalance>('GET', '/api/tender-cash', undefined, token);
