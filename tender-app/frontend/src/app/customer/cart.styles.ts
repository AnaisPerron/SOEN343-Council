import { StyleSheet } from 'react-native';

const font = 'Gotham Rounded';
const ink = '#302629';
const tender = '#D42F50';

export const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#FFFFFF' },
  header: { minHeight: 62, paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', borderBottomWidth: 1, borderBottomColor: '#F0EAEA' },
  backButton: { width: 40, height: 40, justifyContent: 'center' },
  backText: { color: ink, fontSize: 32, lineHeight: 36 },
  title: { flex: 1, fontFamily: font, color: ink, fontSize: 21, fontWeight: '900', textAlign: 'center' },
  headerSpacer: { width: 40 },
  content: { paddingHorizontal: 22, paddingTop: 28, paddingBottom: 60, width: '100%', maxWidth: 760, alignSelf: 'center' },
  count: { fontFamily: font, color: '#887B7E', fontSize: 13, marginBottom: 16 },
  line: { paddingVertical: 20, borderBottomWidth: 1, borderBottomColor: '#F0EAEA' },
  restaurantName: { fontFamily: font, color: '#A92140', fontSize: 11, fontWeight: '800', marginBottom: 7 },
  lineTop: { flexDirection: 'row', justifyContent: 'space-between', gap: 15 },
  itemName: { flex: 1, fontFamily: font, color: ink, fontSize: 17, fontWeight: '800' },
  linePrice: { fontFamily: font, color: ink, fontSize: 15, fontWeight: '800' },
  options: { fontFamily: font, color: '#6E6265', fontSize: 12, lineHeight: 18, marginTop: 7 },
  quantityControl: { flexDirection: 'row', alignItems: 'center', gap: 15, marginTop: 17, alignSelf: 'flex-start' },
  quantityButton: { width: 30, height: 30, borderRadius: 15, backgroundColor: '#FCE7EB', alignItems: 'center', justifyContent: 'center' },
  quantityButtonText: { color: tender, fontSize: 21, fontWeight: '800', lineHeight: 25 },
  quantityText: { fontFamily: font, color: ink, fontSize: 15, fontWeight: '800' },
  totalRow: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 28 },
  totalLabel: { fontFamily: font, color: ink, fontSize: 18, fontWeight: '800' },
  totalValue: { fontFamily: font, color: ink, fontSize: 18, fontWeight: '900' },
  totalNote: { fontFamily: font, color: '#887B7E', fontSize: 12, marginTop: 9 },
  empty: { alignItems: 'center', paddingTop: 90 },
  emptyTitle: { fontFamily: font, color: ink, fontSize: 23, fontWeight: '900' },
  emptyText: { fontFamily: font, color: '#887B7E', fontSize: 14, textAlign: 'center', marginTop: 10 },
  browseButton: { marginTop: 24, borderRadius: 14, backgroundColor: tender, paddingVertical: 14, paddingHorizontal: 20 },
  browseText: { fontFamily: font, color: '#FFFFFF', fontSize: 14, fontWeight: '800' },
});
