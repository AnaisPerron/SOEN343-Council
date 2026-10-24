import { StyleSheet } from 'react-native';

// Same accent as auth.styles.ts and dashboard.styles.ts.
const ACCENT = '#ef4444';

export const styles = StyleSheet.create({
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(17, 24, 39, 0.45)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  profileCard: {
    width: '100%',
    maxWidth: 380,
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 24,
    gap: 4,
  },
  profileCardTitle: {
    fontSize: 22,
    fontWeight: '800',
    color: ACCENT,
    marginBottom: 12,
  },
  profileDetailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  profileLabel: { fontSize: 14, fontWeight: '600', color: '#6b7280' },
  profileValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#111827',
    flexShrink: 1,
    textAlign: 'right',
  },
  logoutButton: {
    marginTop: 20,
    backgroundColor: ACCENT,
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
  },
  logoutButtonText: { color: '#fff', fontSize: 16, fontWeight: '700' },
});