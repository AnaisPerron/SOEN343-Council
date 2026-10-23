import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#FFFFFF' },
  scroll: { flex: 1 },
  scrollContent: { paddingBottom: 88 },
  cover: { position: 'absolute', top: 0, left: 0, right: 0, backgroundColor: '#EBD9D8' },
  coverImage: { width: '100%', height: '100%' },
  coverFallback: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#F7DFE2' },
  coverFallbackText: { fontFamily: 'Gotham Rounded', fontSize: 32, fontWeight: '800', color: '#D42F50' },
  panel: { marginTop: -30, borderTopLeftRadius: 28, borderTopRightRadius: 28, backgroundColor: '#FFFFFF', minHeight: 600, shadowColor: '#20191A', shadowOffset: { width: 0, height: -4 }, shadowOpacity: 0.08, shadowRadius: 16, elevation: 6 },
  backButton: { width: 40, height: 40, borderRadius: 20, backgroundColor: 'rgba(30,25,26,0.66)', alignItems: 'center', justifyContent: 'center' },
  backButtonText: { color: '#FFFFFF', fontSize: 27, fontWeight: '500', lineHeight: 32, marginTop: -2 },
});
