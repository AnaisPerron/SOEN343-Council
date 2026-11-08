import { StyleSheet } from 'react-native';

export const MARKER_SIZE = 18;

// Shape tells restaurants and couriers apart; colour shows their state
export const stateColors: Record<string, string> = {
  open: '#16a34a',
  paused: '#f59e0b',
  closed: '#9ca3af',
  idle: '#16a34a',
  assigned: '#f59e0b',
  carrying: '#2563eb',
};

export const styles = StyleSheet.create({
  wrapper: { marginBottom: 20 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  title: { fontSize: 18, fontWeight: '800', color: '#111827' },
  error: { color: '#b91c1c', marginBottom: 12 },
  measure: { width: '100%' },
  map: {
    alignSelf: 'center',
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#e5e7eb',
    backgroundColor: '#f8fafc',
    overflow: 'hidden',
  },
  gridLine: { position: 'absolute', backgroundColor: '#e5e7eb' },
  marker: {
    position: 'absolute',
    width: MARKER_SIZE,
    height: MARKER_SIZE,
    marginLeft: -MARKER_SIZE / 2,
    marginTop: -MARKER_SIZE / 2,
    borderWidth: 2,
    borderColor: '#fff',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.25,
    shadowRadius: 2,
    elevation: 2,
  },
  restaurantMarker: { borderRadius: 4 },
  courierMarker: { borderRadius: MARKER_SIZE / 2 },
  markerActive: { borderColor: '#111827', zIndex: 2 },
  emptyOverlay: { ...StyleSheet.absoluteFill, alignItems: 'center', justifyContent: 'center', padding: 24 },
  emptyTitle: { fontSize: 16, fontWeight: '700', color: '#374151', textAlign: 'center' },
  emptyText: { marginTop: 4, color: '#6b7280', textAlign: 'center' },
  tooltip: {
    position: 'absolute',
    maxWidth: 220,
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 8,
    backgroundColor: '#111827',
    zIndex: 3,
  },
  tooltipName: { color: '#fff', fontWeight: '700' },
  tooltipState: { color: '#d1d5db', fontSize: 12 },
  legend: { flexDirection: 'row', flexWrap: 'wrap', gap: 14, marginTop: 10 },
  legendItem: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  legendSwatch: { width: 12, height: 12, borderRadius: 3 },
  legendText: { fontSize: 13, color: '#6b7280' },
});
