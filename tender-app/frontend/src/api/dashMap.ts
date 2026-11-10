import { requestJson } from './client.ts';
import { normalizeMapData } from '../lib/cityMap.ts';
import type { MapData } from '../types/dash.ts';

// Short enough that duty changes, dispatch, pickup, delivery and a reset show up while the map is open
export const MAP_REFRESH_MS = 5000;

export const fetchMapData = async (token: string): Promise<MapData> =>
  normalizeMapData(await requestJson<Partial<MapData> | null>('GET', '/api/map', undefined, token));

/**
 * Loads the map now, then again every MAP_REFRESH_MS so it follows commands run anywhere in the system.
 * EventSource cannot attach the bearer token, so the map polls like the other live screens.
 * Returns a function that stops watching.
 */
export function watchMapData(
  token: string,
  onData: (data: MapData) => void,
  onError: (cause: unknown) => void,
  intervalMs = MAP_REFRESH_MS,
): () => void {
  let stopped = false;
  let inFlight = false;
  const load = () => {
    // Skip a tick rather than stack requests on a slow connection
    if (inFlight) return;
    inFlight = true;
    fetchMapData(token).then(
      (data) => { if (!stopped) onData(data); },
      (cause: unknown) => { if (!stopped) onError(cause); },
    ).finally(() => { inFlight = false; });
  };
  load();
  const timer = setInterval(load, intervalMs);
  return () => {
    stopped = true;
    clearInterval(timer);
  };
}
