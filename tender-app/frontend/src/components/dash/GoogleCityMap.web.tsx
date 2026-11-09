import { useEffect } from 'react';
import { AdvancedMarker, APIProvider, Map, useMap } from '@vis.gl/react-google-maps';
import { markerStateLabel, type MapMarker } from '@/lib/cityMap';
import type { MapBounds } from '@/types/dash';
import { MARKER_SIZE, stateColors } from './CityMap.styles';

const apiKey = process.env.EXPO_PUBLIC_GOOGLE_MAPS_API_KEY?.trim() ?? '';
// Advanced markers need a map id; Google's demo id works until the team sets its own
const mapId = process.env.EXPO_PUBLIC_GOOGLE_MAPS_MAP_ID?.trim() || 'DEMO_MAP_ID';
const PADDING = 40;

/** Without a key the dashboard keeps the built-in street map. */
export const googleMapsAvailable = apiKey !== '';

export interface GoogleCityMapProps {
  bounds: MapBounds;
  markers: MapMarker[];
  activeKey: string | null;
  /** Changing this fits the map to the dataset bounds again; polling alone leaves the view where the user put it. */
  fitKey: number;
  onHover: (key: string | null) => void;
  onSelect: (key: string) => void;
  /** The key was rejected or the script failed to load. */
  onFail: () => void;
}

const toLiteral = (bounds: MapBounds) => ({
  north: bounds.maxLatitude,
  south: bounds.minLatitude,
  east: bounds.maxLongitude,
  west: bounds.minLongitude,
});

/** City map (DASH) on Google Maps: same markers, colours and tooltips as the built-in map. */
export default function GoogleCityMap({ bounds, markers, activeKey, fitKey, onHover, onSelect, onFail }: GoogleCityMapProps) {
  useEffect(() => {
    // Google calls this global when the key is invalid or not allowed for this site
    const w = window as typeof window & { gm_authFailure?: () => void };
    w.gm_authFailure = onFail;
    return () => { delete w.gm_authFailure; };
  }, [onFail]);

  return (
    <APIProvider apiKey={apiKey} onError={onFail}>
      <Map
        mapId={mapId}
        defaultBounds={{ ...toLiteral(bounds), padding: PADDING }}
        gestureHandling="cooperative"
        clickableIcons={false}
        disableDefaultUI
        zoomControl
        style={{ width: '100%', height: '100%' }}
      >
        <FitBounds bounds={bounds} fitKey={fitKey} />
        {markers.map((marker) => (
          <AdvancedMarker
            key={marker.key}
            position={{ lat: marker.latitude, lng: marker.longitude }}
            title={`${marker.name}, ${markerStateLabel(marker)}`}
            anchorLeft="-50%"
            anchorTop="-50%"
            zIndex={marker.key === activeKey ? 2 : marker.kind === 'courier' ? 1 : 0}
            onClick={() => onSelect(marker.key)}
            onMouseEnter={() => onHover(marker.key)}
            onMouseLeave={() => onHover(null)}
          >
            <MarkerIcon marker={marker} active={marker.key === activeKey} />
          </AdvancedMarker>
        ))}
      </Map>
    </APIProvider>
  );
}

function FitBounds({ bounds, fitKey }: { bounds: MapBounds; fitKey: number }) {
  const map = useMap();
  // Bounds are read when fitKey changes, not on every poll, so couriers moving don't pull the view around
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { map?.fitBounds(toLiteral(bounds), PADDING); }, [map, fitKey]);
  return null;
}

function MarkerIcon({ marker, active }: { marker: MapMarker; active: boolean }) {
  return (
    <div style={{ position: 'relative' }}>
      <div
        style={{
          width: MARKER_SIZE,
          height: MARKER_SIZE,
          boxSizing: 'border-box',
          borderRadius: marker.kind === 'restaurant' ? 4 : MARKER_SIZE / 2,
          border: `2px solid ${active ? '#111827' : '#fff'}`,
          backgroundColor: stateColors[marker.state] ?? '#6b7280',
          boxShadow: '0 1px 2px rgba(0, 0, 0, 0.35)',
          cursor: 'pointer',
        }}
      />
      {active && (
        <div
          style={{
            position: 'absolute',
            bottom: MARKER_SIZE + 6,
            left: '50%',
            transform: 'translateX(-50%)',
            whiteSpace: 'nowrap',
            padding: '6px 10px',
            borderRadius: 8,
            backgroundColor: '#111827',
            fontFamily: 'system-ui, sans-serif',
            pointerEvents: 'none',
          }}
        >
          <div style={{ color: '#fff', fontWeight: 700, fontSize: 14 }}>{marker.name}</div>
          <div style={{ color: '#d1d5db', fontSize: 12 }}>{markerStateLabel(marker)}</div>
        </div>
      )}
    </div>
  );
}
