import { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Image, Pressable, Text, View } from 'react-native';
import { useAuth, type Role } from '@/context/AuthContext';
import { ApiError } from '@/api/client';
import { watchMapData } from '@/api/dashMap';
import { createProjection, fitMapSize, isMapEmpty, mapTiles, markerStateLabel, toMarkers, type MapMarker } from '@/lib/cityMap';
import type { MapBounds, MapData, MapRestaurant } from '@/types/dash';
import { queueBand, queueBandColor } from '@/lib/queueBands';
import { MARKER_SIZE, stateColors, styles } from './CityMap.styles';
import GoogleCityMap, { googleMapsAvailable } from './GoogleCityMap';

const MAX_HEIGHT = 480;

const markerShape = { restaurant: styles.restaurantMarker, courier: styles.courierMarker, customer: styles.customerMarker };

const square = (color: string) => [styles.legendSwatch, { backgroundColor: color }];
const circle = (color: string) => [styles.legendSwatch, { borderRadius: 6, backgroundColor: color }];
const diamond = (color: string) => [styles.legendSwatch, { borderRadius: 2, transform: [{ rotate: '45deg' as const }], backgroundColor: color }];

type LegendItem = { kind: MapMarker['kind']; label: string; style: ReturnType<typeof square> };
const legendSections: { kind: MapMarker['kind']; title: string }[] = [
  { kind: 'restaurant', title: 'Restaurants' },
  { kind: 'courier', title: 'Couriers' },
  { kind: 'customer', title: 'Customers' },
];

/** What each role's legend shows, and the colour a marker takes when its state isn't worth a colour of its own. */
interface RoleView {
  legend: LegendItem[];
  colorOf?: (marker: MapMarker) => string | undefined;
}

// Each role only gets the markers it needs (see MapService), so the legend only explains those
const roleViews: Record<Role, RoleView> = {
  // Manages every restaurant and courier, so their states are the point
  Operator: {
    legend: [
      { kind: 'restaurant', label: 'Short queue (0–2)', style: square(queueBandColor.short) },
      { kind: 'restaurant', label: 'Building queue (3–5)', style: square(queueBandColor.building) },
      { kind: 'restaurant', label: 'Long queue (6+ or paused)', style: square(queueBandColor.long) },
      { kind: 'courier', label: 'Idle courier', style: circle(stateColors.idle) },
      { kind: 'courier', label: 'Assigned courier', style: circle(stateColors.assigned) },
      { kind: 'courier', label: 'Carrying courier', style: circle(stateColors.carrying) },
      { kind: 'customer', label: 'Customer', style: diamond(stateColors.customer) },
    ],
  },
  // Only open restaurants and the courier bringing their order
  customer: {
    legend: [
      { kind: 'restaurant', label: 'Short queue (0–2)', style: square(queueBandColor.short) },
      { kind: 'restaurant', label: 'Building queue (3–5)', style: square(queueBandColor.building) },
      { kind: 'restaurant', label: 'Long queue (6+ or paused)', style: square(queueBandColor.long) },
      { kind: 'courier', label: 'Courier', style: circle(stateColors.assigned) },
      { kind: 'customer', label: 'You', style: diamond(stateColors.customer) },
    ],
    colorOf: (m) => (m.kind === 'courier' ? stateColors.assigned : undefined),
  },
  // Their own position, where to pick up and where to drop off; their state is on the dashboard
  courier: {
    legend: [
      { kind: 'restaurant', label: 'Short queue (0–2)', style: square(queueBandColor.short) },
      { kind: 'restaurant', label: 'Building queue (3–5)', style: square(queueBandColor.building) },
      { kind: 'restaurant', label: 'Long queue (6+ or paused)', style: square(queueBandColor.long) },
      { kind: 'courier', label: 'You', style: circle(stateColors.assigned) },
      { kind: 'customer', label: 'Drop-off customer', style: diamond(stateColors.customer) },
    ],
    colorOf: (m) => (m.kind === 'courier' ? stateColors.assigned : undefined),
  },
  // Their own restaurant, and whether each courier is still coming or has left with the order
  restaurant: {
    legend: [
      { kind: 'restaurant', label: 'Short queue (0–2)', style: square(queueBandColor.short) },
      { kind: 'restaurant', label: 'Building queue (3–5)', style: square(queueBandColor.building) },
      { kind: 'restaurant', label: 'Long queue (6+ or paused)', style: square(queueBandColor.long) },
      { kind: 'courier', label: 'Courier on the way', style: circle(stateColors.assigned) },
      { kind: 'courier', label: 'Courier delivering', style: circle(stateColors.carrying) },
    ],
  },
};

// The server only sends what the role may see (see MapService), so the empty message says what would appear
const emptyHint: Record<Role, string> = {
  Operator: 'Restaurants with a location and on-duty couriers appear here.',
  customer: 'Open restaurants and the courier bringing your order appear here.',
  courier: 'Go on duty to appear here; your pickup and drop-off show once you have an order.',
  restaurant: 'Your restaurant and the couriers handling its orders appear here.',
};

interface CityMapProps {
  /** Changing this reloads the map at once, e.g. after Refresh or a command run on the same page. */
  refreshKey?: number;
  /** Fixed viewport for pages that should stay focused on one area. */
  boundsOverride?: MapBounds;
  /** Fill the parent (e.g. a full-screen map beside a side panel) instead of sizing to the data. */
  fill?: boolean;
  /** Shows a Full screen / Exit full screen button; the page decides what full screen means. */
  onToggleFullScreen?: () => void;
  /** When set, pressing a restaurant marker calls this instead of pinning its tooltip, e.g. to open the restaurant. */
  onRestaurantPress?: (restaurantId: number, restaurant: MapRestaurant) => void;
  highlightedKeys?: string[];
  visibleKinds?: MapMarker['kind'][];
  queueCounts?: Record<number, number>;
  onData?: (data: MapData) => void;
  onMarkerPress?: (marker: MapMarker) => void;
}

/** Shared city map (DASH): the locations the logged-in role may see, scaled to the dataset bounds and kept up to date. */
export default function CityMap({ refreshKey = 0, boundsOverride, fill = false, onToggleFullScreen, onRestaurantPress, onMarkerPress, highlightedKeys = [], visibleKinds, queueCounts, onData }: CityMapProps) {
  const { user, token, signOut } = useAuth();
  const view = roleViews[user?.role ?? 'Operator'];
  const [data, setData] = useState<MapData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loadedKey, setLoadedKey] = useState<number | null>(null);
  const [width, setWidth] = useState(0);
  const [height, setHeight] = useState(0);
  const [hovered, setHovered] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  // Falls back to the built-in street map for the rest of the session if Google rejects the key
  const [googleFailed, setGoogleFailed] = useState(false);
  const useGoogle = googleMapsAvailable && !googleFailed;
  const onGoogleFail = useCallback(() => setGoogleFailed(true), []);
  // Spinner only for the first load after a refreshKey change; background refreshes keep the current map up
  const loading = loadedKey !== refreshKey;
  const bounds = boundsOverride ?? data?.bounds;

  useEffect(() => {
    if (!token) return;
    return watchMapData(
      token,
      (next) => {
        setData(next);
        onData?.(next);
        setError(null);
        setLoadedKey(refreshKey);
      },
      (cause) => {
        if (cause instanceof ApiError && cause.status === 401) signOut();
        else setError(cause instanceof Error ? cause.message : 'Unable to load map');
        setLoadedKey(refreshKey);
      },
    );
  }, [token, refreshKey, signOut, onData]);

  // A filled map takes the parent's shape, so the data is projected into that shape rather than the other way round
  const fillAspect = fill && width > 0 && height > 0 ? height / width : undefined;
  const markers = useMemo(() => {
    const all = data && bounds ? toMarkers({ ...data, bounds }, fillAspect).filter((marker) => !visibleKinds || visibleKinds.includes(marker.kind))
      .map((marker) => marker.kind === 'restaurant' && queueCounts?.[marker.id] !== undefined
        ? { ...marker, queueCount: queueCounts[marker.id], color: queueBandColor[queueBand(queueCounts[marker.id], marker.state)] }
        : marker) : [];
    const { colorOf } = view;
    // Colour each marker the way the role's legend explains it
    return colorOf ? all.map((m) => ({ ...m, color: colorOf(m) ?? m.color })) : all;
  }, [data, bounds, fillAspect, view, visibleKinds, queueCounts]);
  const size = !bounds || width <= 0 ? null
    : fill ? (fillAspect ? { width, height } : null)
    : fitMapSize(createProjection(bounds).aspect, width, MAX_HEIGHT);
  const mapWidth = size?.width ?? 0;
  const tiles = useMemo(
    () => (bounds && !useGoogle ? mapTiles(bounds, mapWidth, fillAspect) : []),
    [bounds, mapWidth, useGoogle, fillAspect],
  );
  // Hover previews a marker; a click pins it until clicked again
  const active = markers.find((m) => m.key === (hovered ?? highlightedKeys[0] ?? selected));
  const highlighted = new Set(highlightedKeys);
  const legend = view.legend.filter((item) => markers.some((marker) => {
    if (marker.kind !== item.kind) return false;
    if (item.label.startsWith('Short queue')) return marker.color === queueBandColor.short;
    if (item.label.startsWith('Building queue')) return marker.color === queueBandColor.building;
    if (item.label.startsWith('Long queue')) return marker.color === queueBandColor.long;
    if (item.label === 'Courier on the way' || item.label === 'Assigned courier') return marker.state === 'assigned';
    if (item.label === 'Courier delivering' || item.label === 'Carrying courier') return marker.state === 'carrying';
    if (item.label === 'Idle courier') return marker.state === 'idle';
    return true;
  }));
  const legendColumns = legendSections.map((section) => ({
    ...section, items: legend.filter((item) => item.kind === section.kind),
  })).filter((section) => section.items.length > 0);
  const pressMarker = (key: string) => {
    const marker = markers.find((m) => m.key === key);
    if (marker) onMarkerPress?.(marker);
    if (marker?.kind === 'restaurant' && onRestaurantPress) {
      const restaurant = data?.restaurants.find((item) => item.id === marker.id);
      if (restaurant) onRestaurantPress(marker.id, { ...restaurant, queueCount: marker.queueCount ?? restaurant.queueCount });
    }
    else setSelected((current) => (current === key ? null : key));
  };

  return (
    <View style={fill ? styles.fillWrapper : styles.wrapper}>
      {!fill && (
        <View style={styles.header}>
          <Text style={styles.title}>City map</Text>
          <View style={styles.headerActions}>
            {loading && <ActivityIndicator accessibilityLabel="Loading map" color="#D42F50" />}
            {onToggleFullScreen && (
              <Pressable onPress={onToggleFullScreen} accessibilityRole="button" style={styles.fullScreenButton}>
                <Text style={styles.fullScreenText}>⤢ Full screen</Text>
              </Pressable>
            )}
          </View>
        </View>
      )}
      {error && <Text accessibilityRole="alert" style={[styles.error, fill && styles.fillError]}>{error}</Text>}
      <View
        style={fill ? styles.fillMeasure : styles.measure}
        onLayout={(event) => {
          setWidth(event.nativeEvent.layout.width);
          setHeight(event.nativeEvent.layout.height);
        }}
      >
        {size && (
          <View style={[styles.map, fill && styles.fillMap, size]}>
            {useGoogle && data ? (
              <GoogleCityMap
                bounds={bounds!}
                restrictBounds={boundsOverride !== undefined}
                markers={markers}
                activeKey={active?.key ?? null}
                highlightedKeys={highlightedKeys}
                fitKey={refreshKey}
                onHover={setHovered}
                onSelect={pressMarker}
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
                    onPress={() => pressMarker(marker.key)}
                    onHoverIn={() => setHovered(marker.key)}
                    onHoverOut={() => setHovered((current) => (current === marker.key ? null : current))}
                    accessibilityRole="button"
                    accessibilityLabel={marker.kind === 'restaurant'
                      ? `${marker.name} · ${marker.queueCount ?? 0} queued, ${markerStateLabel(marker)}`
                      : `${marker.name}, ${markerStateLabel(marker)}`}
                    hitSlop={6}
                    style={[
                      styles.marker,
                      markerShape[marker.kind],
                      { left: `${marker.x * 100}%`, top: `${marker.y * 100}%`, backgroundColor: marker.color ?? stateColors[marker.state] ?? '#6b7280' },
                      (active?.key === marker.key || highlighted.has(marker.key)) && styles.markerActive,
                      highlighted.has(marker.key) && styles.markerHighlighted,
                    ]}
                  />
                ))}
                {active && <MarkerTooltip marker={active} />}
              </>
            )}
            {data && isMapEmpty(data) && (
              <View pointerEvents="none" style={styles.emptyOverlay}>
                <Text style={styles.emptyTitle}>Nothing to show on the map yet</Text>
                {user && <Text style={styles.emptyText}>{emptyHint[user.role]}</Text>}
              </View>
            )}
          </View>
        )}
        {fill && loading && <ActivityIndicator accessibilityLabel="Loading map" color="#D42F50" style={styles.fillSpinner} />}
        {fill && onToggleFullScreen && (
          <Pressable onPress={onToggleFullScreen} accessibilityRole="button" style={[styles.fullScreenButton, styles.exitFullScreen]}>
            <Text style={styles.fullScreenText}>⤡ Exit full screen</Text>
          </Pressable>
        )}
      </View>
      {size && legendColumns.length > 0 && (
        <View style={[styles.legend, fill && styles.fillLegend]} pointerEvents="none" accessibilityRole="table">
          {legendColumns.map((column) => (
            <View key={column.kind} style={styles.legendColumn}>
              <Text style={styles.legendHeader}>{column.title}</Text>
              {column.items.map((item) => (
                <View key={item.label} style={styles.legendItem} accessibilityRole="row">
                  <View style={item.style} />
                  <Text style={styles.legendText}>{item.label}</Text>
                </View>
              ))}
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
  const vertical = { bottom: `${(1 - marker.y) * 100}%` as const, marginBottom: gap };
  return (
    <View pointerEvents="none" style={[styles.tooltip, horizontal, vertical]}>
      <Text style={styles.tooltipName}>{marker.kind === 'restaurant' ? `${marker.name} · ${marker.queueCount ?? 0} queued` : marker.name}</Text>
      <Text style={styles.tooltipState}>{markerStateLabel(marker)}</Text>
    </View>
  );
}
