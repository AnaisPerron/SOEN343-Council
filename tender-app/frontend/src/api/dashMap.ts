import { requestJson } from './client.ts';
import { normalizeMapData } from '../lib/cityMap.ts';
import type { MapData } from '../types/dash.ts';

export const fetchMapData = async (token: string): Promise<MapData> =>
  normalizeMapData(await requestJson<Partial<MapData> | null>('GET', '/api/map', undefined, token));
