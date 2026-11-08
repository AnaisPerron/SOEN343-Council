import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Pressable, Text, View } from 'react-native';
import { useAuth } from '@/context/AuthContext';
import { ApiError } from '@/api/client';
import { fetchMapData } from '@/api/dashMap';
import { createProjection, fitMapSize, isMapEmpty, markerStateLabel, toMarkers, type MapMarker } from '@/lib/cityMap';
import type { MapData } from '@/types/dash';
import { MARKER_SIZE, stateColors, styles } from './CityMap.styles';

const MAX_HEIGHT = 480;
const GRID_LINES = [0.25, 0.5, 0.75];

const legend = [
  { label: 'Restaurant', style: [styles.legendSwatch, { backgroundColor: '#6b7280' }] },
  { label: 'Courier', style: [styles.legendSwatch, { borderRadius: 6, backgroundColor: '#6b7280' }] },
  { label: 'Open / Idle', style: [styles.legendSwatch, { backgroundColor: stateColors.open }] },
  { label: 'Paused / Assigned', style: [styles.legendSwatch, { backgroundColor: stateColors.paused }] },
  { label: 'Closed', style: [styles.legendSwatch, { backgroundColor: stateColors.closed }] },
  { label: 'Carrying', style: [styles.legendSwatch, { backgroundColor: stateColors.carrying }] },
];

interface CityMapProps {
  /** Changing this reloads the map, e.g. when the page's own Refresh is pressed. */
  refreshKey?: number;
}

/** Shared city map (DASH): restaurants and on-duty couriers, scaled to the dataset bounds. */
export default function CityMap({ refreshKey = 0 }: CityMapProps) {
  const { token, signOut } = useAuth();
  const [data, setData] = useState<MapData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loadedKey, setLoadedKey] = useState<number | null>(null);
  const [width, setWidth] = useState(0);
  const [hovered, setHovered] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  // Still loading until the request for the current refreshKey answers; the last map stays up meanwhile
  const loading = loadedKey !== refreshKey;

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    fetchMapData(token).then(
      (next) => {
        if (cancelled) return;
        setData(next);
        setError(null);
        setLoadedKey(refreshKey);
      },
      (cause: unknown) => {
        if (cancelled) return;
        if (cause instanceof ApiError && cause.status === 401) signOut();
        else setError(cause instanceof Error ? cause.message : 'Unable to load map');
        setLoadedKey(refreshKey);
      },
    );
    return () => { cancelled = true; };
  }, [token, refreshKey, signOut]);

  const markers = useMemo(() => (data ? toMarkers(data) : []), [data]);
  const size = data && width > 0 ? fitMapSize(createProjection(data.bounds).aspect, width, MAX_HEIGHT) : null;
  // Hover previews a marker; a click pins it until clicked again
  const active = markers.find((m) => m.key === (hovered ?? selected));

  return (
    <View style={styles.wrapper}>
      <View style={styles.header}>
        <Text style={styles.title}>City map</Text>
        {loading && <ActivityIndicator accessibilityLabel="Loading map" color="#D42F50" />}
      </View>
      {error && <Text accessibilityRole="alert" style={styles.error}>{error}</Text>}
      <View style={styles.measure} onLayout={(event) => setWidth(event.nativeEvent.layout.width)}>
        {size && (
          <View style={[styles.map, size]}>
            {GRID_LINES.flatMap((at) => [
              <View key={`col-${at}`} pointerEvents="none" style={[styles.gridLine, { left: `${at * 100}%`, top: 0, bottom: 0, width: 1 }]} />,
              <View key={`row-${at}`} pointerEvents="none" style={[styles.gridLine, { top: `${at * 100}%`, left: 0, right: 0, height: 1 }]} />,
            ])}
            {markers.map((marker) => (
              <Pressable
                key={marker.key}
                onPress={() => setSelected((current) => (current === marker.key ? null : marker.key))}
                onHoverIn={() => setHovered(marker.key)}
                onHoverOut={() => setHovered((current) => (current === marker.key ? null : current))}
                accessibilityRole="button"
                accessibilityLabel={`${marker.name}, ${markerStateLabel(marker)}`}
                hitSlop={6}
                style={[
                  styles.marker,
                  marker.kind === 'restaurant' ? styles.restaurantMarker : styles.courierMarker,
                  { left: `${marker.x * 100}%`, top: `${marker.y * 100}%`, backgroundColor: stateColors[marker.state] ?? '#6b7280' },
                  active?.key === marker.key && styles.markerActive,
                ]}
              />
            ))}
            {active && <MarkerTooltip marker={active} />}
            {data && isMapEmpty(data) && (
              <View pointerEvents="none" style={styles.emptyOverlay}>
                <Text style={styles.emptyTitle}>No restaurants or couriers to show yet</Text>
                <Text style={styles.emptyText}>Restaurants with a location and on-duty couriers appear here.</Text>
              </View>
            )}
          </View>
        )}
      </View>
      {size && (
        <View style={styles.legend}>
          {legend.map((item) => (
            <View key={item.label} style={styles.legendItem}>
              <View style={item.style} />
              <Text style={styles.legendText}>{item.label}</Text>
            </View>
          ))}
        </View>
      )}
    </View>
  );
}

// Opens toward the centre of the map so it never runs off an edge
function MarkerTooltip({ marker }: { marker: MapMarker }) {
  const gap = MARKER_SIZE / 2 + 6;
  const horizontal = marker.x > 0.5 ? { right: `${(1 - marker.x) * 100}%` as const } : { left: `${marker.x * 100}%` as const };
  const vertical = marker.y > 0.5
    ? { bottom: `${(1 - marker.y) * 100}%` as const, marginBottom: gap }
    : { top: `${marker.y * 100}%` as const, marginTop: gap };
  return (
    <View pointerEvents="none" style={[styles.tooltip, horizontal, vertical]}>
      <Text style={styles.tooltipName}>{marker.name}</Text>
      <Text style={styles.tooltipState}>{markerStateLabel(marker)}</Text>
    </View>
  );
}
