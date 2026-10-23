import { StyleSheet } from 'react-native';

const font = 'Gotham Rounded';
const ink = '#302629';

export const styles = StyleSheet.create({
  coverControls: { position: 'absolute', left: 18, right: 18 },
  stickyHeader: { position: 'absolute', top: 0, left: 0, right: 0, minHeight: 58, paddingHorizontal: 18, paddingBottom: 10, backgroundColor: '#FFFFFF', flexDirection: 'row', alignItems: 'center', gap: 14, shadowColor: '#372B2E', shadowOffset: { width: 0, height: 3 }, shadowOpacity: 0.1, shadowRadius: 8, elevation: 8 },
  stickyTitle: { flex: 1, fontFamily: font, fontSize: 16, fontWeight: '800', color: ink },
  info: { paddingTop: 26, paddingHorizontal: 22, paddingBottom: 28, borderBottomWidth: 1, borderBottomColor: '#F0EAEA', maxWidth: 800, width: '100%', alignSelf: 'center' },
  restaurantName: { fontFamily: font, color: '#A92140', fontSize: 12, fontWeight: '800', marginBottom: 10 },
  title: { fontFamily: font, color: ink, fontSize: 29, fontWeight: '900', lineHeight: 35, letterSpacing: -0.8 },
  description: { fontFamily: font, color: '#6E6265', fontSize: 14, lineHeight: 21, marginTop: 12 },
  price: { fontFamily: font, color: ink, fontSize: 21, fontWeight: '800', marginTop: 20 },
  unavailable: { fontFamily: font, color: '#A92140', fontSize: 13, fontWeight: '800', marginTop: 13 },
  options: { paddingHorizontal: 22, paddingTop: 24, maxWidth: 800, width: '100%', alignSelf: 'center' },
  optionsHeading: { fontFamily: font, color: ink, fontSize: 20, fontWeight: '800', marginBottom: 8 },
  optionRow: { paddingVertical: 15, borderBottomWidth: 1, borderBottomColor: '#F0EAEA', flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 16 },
  optionName: { flex: 1, fontFamily: font, color: ink, fontSize: 14 },
  optionCost: { fontFamily: font, color: '#6E6265', fontSize: 13, fontWeight: '700' },
  emptyOptions: { fontFamily: font, color: '#887B7E', fontSize: 13, marginTop: 10 },
  notFound: { flex: 1, padding: 24, alignItems: 'center', justifyContent: 'center', backgroundColor: '#FFFFFF' },
  backLink: { fontFamily: font, color: '#D42F50', marginTop: 16, fontSize: 15 },
});
