import { StyleSheet } from 'react-native';

const font = 'Gotham Rounded';
const ink = '#302629';
const tender = '#D42F50';

export const styles = StyleSheet.create({
  coverControls: { position: 'absolute', left: 18, right: 18, flexDirection: 'row', justifyContent: 'space-between' },
  coverCart: { minWidth: 68, height: 40, borderRadius: 20, backgroundColor: 'rgba(30,25,26,0.66)', alignItems: 'center', justifyContent: 'center', paddingHorizontal: 12 },
  coverCartText: { fontFamily: font, color: '#FFFFFF', fontSize: 12, fontWeight: '800' },
  stickyHeader: { position: 'absolute', top: 0, left: 0, right: 0, minHeight: 58, paddingHorizontal: 18, paddingBottom: 10, backgroundColor: '#FFFFFF', flexDirection: 'row', alignItems: 'center', gap: 14, shadowColor: '#372B2E', shadowOffset: { width: 0, height: 3 }, shadowOpacity: 0.1, shadowRadius: 8, elevation: 8 },
  stickyTitle: { flex: 1, fontFamily: font, fontSize: 16, fontWeight: '800', color: ink },
  stickyCart: { backgroundColor: '#FCE7EB', borderRadius: 18, paddingHorizontal: 12, paddingVertical: 9 },
  stickyCartText: { fontFamily: font, color: '#A92140', fontSize: 11, fontWeight: '800' },
  itemPanel: { paddingTop: 28, paddingHorizontal: 22, paddingBottom: 170, maxWidth: 800, width: '100%', alignSelf: 'center', alignItems: 'center' },
  restaurantName: { fontFamily: font, color: '#A92140', fontSize: 12, fontWeight: '800', marginBottom: 12 },
  title: { fontFamily: font, color: ink, fontSize: 29, fontWeight: '900', lineHeight: 35, letterSpacing: -0.8, textAlign: 'center' },
  price: { fontFamily: font, color: ink, fontSize: 17, fontWeight: '800', marginTop: 13 },
  description: { fontFamily: font, color: '#6E6265', fontSize: 15, lineHeight: 23, marginTop: 25, textAlign: 'center', maxWidth: 530 },
  unavailable: { fontFamily: font, color: '#A92140', fontSize: 13, fontWeight: '800', marginTop: 16 },
  customizeSection: { width: '100%', maxWidth: 560, marginTop: 35 },
  customizeButton: { minHeight: 75, borderRadius: 19, backgroundColor: '#F7F3F3', paddingHorizontal: 21, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  customizeText: { fontFamily: font, color: ink, fontSize: 16, fontWeight: '700' },
  customizeArrow: { color: ink, fontSize: 31, lineHeight: 34 },
  customizeSummary: { fontFamily: font, color: '#6E6265', fontSize: 12, lineHeight: 18, marginTop: 10, paddingHorizontal: 8 },
  quantityControl: { marginTop: 34, borderRadius: 24, backgroundColor: '#FFF1F3', paddingHorizontal: 15, paddingVertical: 10, flexDirection: 'row', alignItems: 'center', gap: 21 },
  quantityButton: { width: 28, height: 28, borderRadius: 14, backgroundColor: '#502F37', alignItems: 'center', justifyContent: 'center' },
  quantityButtonMinimum: { backgroundColor: '#C9B9BD' },
  quantityButtonText: { color: '#FFFFFF', fontSize: 20, fontWeight: '700', lineHeight: 23 },
  quantityText: { fontFamily: font, color: ink, fontSize: 18, fontWeight: '800', minWidth: 16, textAlign: 'center' },
  bottomBar: { position: 'absolute', bottom: 0, left: 0, right: 0, backgroundColor: '#FFFFFF', borderTopWidth: 1, borderTopColor: '#F0EAEA', paddingHorizontal: 14, paddingTop: 12, alignItems: 'center' },
  addToCart: { width: '100%', maxWidth: 760, minHeight: 60, borderRadius: 17, backgroundColor: tender, alignItems: 'center', justifyContent: 'center' },
  addToCartUnavailable: { backgroundColor: '#BFB5B7' },
  addToCartText: { fontFamily: font, color: '#FFFFFF', fontSize: 18, fontWeight: '800' },
  orderErrorOverlay: { position: 'absolute', top: 0, right: 0, bottom: 0, left: 0, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24, zIndex: 10 },
  orderErrorToast: { width: '100%', maxWidth: 420, paddingVertical: 18, paddingHorizontal: 22, backgroundColor: '#FFE6EB', borderColor: '#EF9AAA', borderWidth: 1, borderRadius: 18, shadowColor: '#852B3F', shadowOffset: { width: 0, height: 5 }, shadowOpacity: 0.18, shadowRadius: 12, elevation: 8 },
  orderErrorCode: { fontFamily: font, color: '#A92140', fontSize: 15, fontWeight: '900', textAlign: 'center' },
  orderErrorMessage: { fontFamily: font, color: '#5C343B', fontSize: 14, lineHeight: 20, marginTop: 6, textAlign: 'center' },
  notFound: { flex: 1, padding: 24, alignItems: 'center', justifyContent: 'center', backgroundColor: '#FFFFFF' },
  backLink: { fontFamily: font, color: tender, marginTop: 16, fontSize: 15 },
});
