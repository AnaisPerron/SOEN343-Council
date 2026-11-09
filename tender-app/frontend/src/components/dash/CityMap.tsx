import { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Image, Pressable, Text, View } from 'react-native';
import { useAuth } from '@/context/AuthContext';
import { ApiError } from '@/api/client';
import { watchMapData } from '@/api/dashMap';
import { createProjection, fitMapSize, isMapEmpty, mapTiles, markerStateLabel, toMarkers, type MapMarker } from '@/lib/cityMap';
import type { MapData } from '@/types/dash';
import { MARKER_SIZE, stateColors, styles } from './CityMap.styles';
import GoogleCityMap, { googleMapsAvailable } from './GoogleCityMap';

const MAX_HEIGHT = 480;

const legend = [
  { label: 'Restaurant', style: [styles.legendSwatch, { backgroundColor: '#6b7280' }] },
  { label: 'Courier', style: [styles.legendSwatch, { borderRadius: 6, backgroundColor: '#6b7280' }] },
  { label: 'Open / Idle', style: [styles.legendSwatch, { backgroundColor: stateColors.open }] },
  { label: 'Paused / Assigned', style: [styles.legendSwatch, { backgroundColor: stateColors.paused }] },
  { label: 'Closed', style: [styles.legendSwatch, { backgroundColor: stateColors.closed }] },
  { label: 'Carrying', style: [styles.legendSwatch, { backgroundColor: stateColors.carrying }] },
];

interface CityMapProps {
  /** Changing this reloads the map at once, e.g. after Refresh or a command run on the same page. */
  refreshKey?: number;
}

/** Shared city map (DASH): restaurants and on-duty couriers, scaled to the dataset bounds and kept up to date. */
export default function CityMap({ refreshKey = 0 }: CityMapProps) {
  const { token, signOut } = useAuth();
  const [data, setData] = useState<MapData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loadedKey, setLoadedKey] = useState<number | null>(null);
  const [width, setWidth] = useState(0);
  const [hovered, setHovered] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  // Falls back to the built-in street map for the rest of the session if Google rejects the key
  const [googleFailed, setGoogleFailed] = useState(false);
  const useGoogle = googleMapsAvailable && !googleFailed;
  const onGoogleFail = useCallback(() => setGoogleFailed(true), []);
  // Spinner only for the first load after a refreshKey change; background refreshes keep the current map up
  const loading = loadedKey !== refreshKey;

  useEffect(() => {
    if (!token) return;
    return watchMapData(
      token,
      (next) => {
        setData(next);
        setError(null);
        setLoadedKey(refreshKey);
      },
      (cause) => {
        if (cause instanceof ApiError && cause.status === 401) signOut();
        else setError(cause instanceof Error ? cause.message : 'Unable to load map');
        setLoadedKey(refreshKey);
      },
    );
  }, [token, refreshKey, signOut]);

  const markers = useMemo(() => (data ? toMarkers(data) : []), [data]);
  const size = data && width > 0 ? fitMapSize(createProjection(data.bounds).aspect, width, MAX_HEIGHT) : null;
  const mapWidth = size?.width ?? 0;
  const tiles = useMemo(() => (data && !useGoogle ? mapTiles(data.bounds, mapWidth) : []), [data, mapWidth, useGoogle]);
  // Hover previews a marker; a click pins it until clicked again
  const active = markers.find((m) => m.key === (hovered ?? selected));
  const toggleSelected = (key: string) => setSelected((current) => (current === key ? null : key));

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
            {useGoogle && data ? (
              <GoogleCityMap
                bounds={data.bounds}
                markers={markers}
                activeKey={active?.key ?? null}
                fitKey={refreshKey}
                onHover={setHovered}
                onSelect={toggleSelected}
                onFail={onGoogleFail}
              />
            ) : (
              <>
                {tiles.map((tile) => {
                  // Whole pixels so neighbouring tiles meet without hairline seams
                  const left = Math.floor(tile.x * size.width);
                  const top = Math.floor(tile.y * size.height);
                  return (
                    <Image
                      key={tile.key}
                      source={{ uri: tile.url }}
                      pointerEvents="none"
                      style={[styles.tile, { left, top, width: Math.ceil((tile.x + tile.width) * size.width) - left, height: Math.ceil((tile.y + tile.height) * size.height) - top }]}
                    />
                  );
                })}
                <Text pointerEvents="none" style={styles.attribution}>© OpenStreetMap contributors</Text>
                {markers.map((marker) => (
                  <Pressable
                    key={marker.key}
                    onPress={() => toggleSelected(marker.key)}
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
              </>
            )}
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
