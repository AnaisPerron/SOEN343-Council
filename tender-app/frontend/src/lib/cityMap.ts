import type { MapBounds, MapData } from '../types/dash.ts';

// Same area as the backend's MapService.DEFAULT_BOUNDS (central Montreal)
export const DEFAULT_MAP_BOUNDS: MapBounds = { minLatitude: 45.49, minLongitude: -73.62, maxLatitude: 45.54, maxLongitude: -73.55 };

/** Fills in anything missing from the response so an empty dataset still draws a map instead of failing. */
export function normalizeMapData(data: Partial<MapData> | null | undefined): MapData {
  return {
    bounds: data?.bounds ?? DEFAULT_MAP_BOUNDS,
    restaurants: data?.restaurants ?? [],
    couriers: data?.couriers ?? [],
  };
}

export const isMapEmpty = (data: MapData) => data.restaurants.length === 0 && data.couriers.length === 0;

// Keeps a lone marker (or markers on one street) from filling the whole map, about 500 m
const MIN_SPAN = 0.005;
// Empty margin around the outermost markers, as a share of the span
const PADDING = 0.1;
// Very wide or very tall datasets are padded out so the map never becomes a thin strip
const MIN_ASPECT = 0.4;
const MAX_ASPECT = 1.5;

export interface MapProjection {
  /** Height divided by width of the drawn area. */
  aspect: number;
  /** Position as a share (0 to 1) of the map's width and height; y grows downward. */
  project: (latitude: number, longitude: number) => { x: number; y: number };
}

/**
 * Fits the dataset bounds into the map. A degree of longitude is shorter than a degree of latitude away
 * from the equator, so longitudes are scaled by cos(latitude) to keep distances true in both directions.
 */
export function createProjection(bounds: MapBounds): MapProjection {
  const midLatitude = (bounds.minLatitude + bounds.maxLatitude) / 2;
  const midLongitude = (bounds.minLongitude + bounds.maxLongitude) / 2;
  const lonScale = Math.cos((midLatitude * Math.PI) / 180);

  let spanX = Math.max((bounds.maxLongitude - bounds.minLongitude) * lonScale, MIN_SPAN) * (1 + 2 * PADDING);
  let spanY = Math.max(bounds.maxLatitude - bounds.minLatitude, MIN_SPAN) * (1 + 2 * PADDING);
  spanY = Math.max(spanY, spanX * MIN_ASPECT);
  spanX = Math.max(spanX, spanY / MAX_ASPECT);

  return {
    aspect: spanY / spanX,
    project: (latitude, longitude) => ({
      x: 0.5 + ((longitude - midLongitude) * lonScale) / spanX,
      y: 0.5 - (latitude - midLatitude) / spanY,
    }),
  };
}

/** Largest map size with the projection's aspect that fits in the available space. */
export function fitMapSize(aspect: number, maxWidth: number, maxHeight: number) {
  const width = Math.min(maxWidth, maxHeight / aspect);
  return { width, height: width * aspect };
}

export type MapMarker = {
  key: string;
  kind: 'restaurant' | 'courier';
  name: string;
  state: string;
  x: number;
  y: number;
};

/** Every restaurant and courier as a positioned marker; restaurants first so couriers draw on top. */
export function toMarkers(data: MapData): MapMarker[] {
  const { project } = createProjection(data.bounds);
  return [
    ...data.restaurants.map((r) => ({ key: `restaurant-${r.id}`, kind: 'restaurant' as const, name: r.name, state: r.status, ...project(r.latitude, r.longitude) })),
    ...data.couriers.map((c) => ({ key: `courier-${c.courierId}`, kind: 'courier' as const, name: c.name, state: c.status, ...project(c.latitude, c.longitude) })),
  ];
}

/** e.g. "Restaurant · Open" or "Courier · Carrying". */
export function markerStateLabel(marker: Pick<MapMarker, 'kind' | 'state'>) {
  const kind = marker.kind === 'restaurant' ? 'Restaurant' : 'Courier';
  return `${kind} · ${marker.state.charAt(0).toUpperCase()}${marker.state.slice(1)}`;
}
