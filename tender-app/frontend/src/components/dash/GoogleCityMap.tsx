import type { MapMarker } from '@/lib/cityMap';
import type { MapBounds } from '@/types/dash';

// The Google Maps JavaScript API only runs in a browser; phones use the built-in street map (see GoogleCityMap.web.tsx)
export const googleMapsAvailable = false;

export interface GoogleCityMapProps {
  bounds: MapBounds;
  markers: MapMarker[];
  activeKey: string | null;
  highlightedKeys: string[];
  fitKey: number;
  onHover: (key: string | null) => void;
  onSelect: (key: string) => void;
  onFail: () => void;
}

export default function GoogleCityMap(_props: GoogleCityMapProps) {
  return null;
}
