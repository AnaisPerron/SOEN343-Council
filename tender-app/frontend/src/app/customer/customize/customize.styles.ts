import { StyleSheet } from 'react-native';

const font = 'Gotham Rounded';
const ink = '#302629';
const tender = '#D42F50';

export const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#FFFFFF' },
  header: { minHeight: 62, paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', borderBottomWidth: 1, borderBottomColor: '#F0EAEA' },
  backButton: { width: 40, height: 40, justifyContent: 'center' },
  backText: { color: ink, fontSize: 32, lineHeight: 36 },
  headerTitle: { flex: 1, fontFamily: font, color: ink, fontSize: 21, fontWeight: '900', textAlign: 'center' },
  headerSpacer: { width: 40 },
  content: { width: '100%', maxWidth: 760, alignSelf: 'center', paddingHorizontal: 22, paddingTop: 28, paddingBottom: 40 },
  itemName: { fontFamily: font, color: ink, fontSize: 25, fontWeight: '900', lineHeight: 31 },
  intro: { fontFamily: font, color: '#6E6265', fontSize: 14, lineHeight: 20, marginTop: 9, marginBottom: 23 },
  optionRow: { minHeight: 70, flexDirection: 'row', alignItems: 'center', gap: 13, borderBottomWidth: 1, borderBottomColor: '#F0EAEA' },
  optionCheck: { width: 24, height: 24, borderWidth: 1, borderColor: '#BAA9AD', borderRadius: 6, alignItems: 'center', justifyContent: 'center' },
  optionCheckSelected: { backgroundColor: tender, borderColor: tender },
  optionCheckMark: { color: '#FFFFFF', fontSize: 16, fontWeight: '800' },
  optionName: { flex: 1, fontFamily: font, color: ink, fontSize: 15, fontWeight: '700' },
  optionCost: { fontFamily: font, color: '#6E6265', fontSize: 13, fontWeight: '700' },
  summary: { marginTop: 28, flexDirection: 'row', justifyContent: 'space-between' },
  summaryLabel: { fontFamily: font, color: ink, fontSize: 16, fontWeight: '800' },
  summaryValue: { fontFamily: font, color: ink, fontSize: 16, fontWeight: '800' },
  empty: { fontFamily: font, color: '#887B7E', fontSize: 14, marginTop: 12 },
  footer: { borderTopWidth: 1, borderTopColor: '#F0EAEA', paddingHorizontal: 14, paddingTop: 12, paddingBottom: 12, alignItems: 'center' },
  saveButton: { width: '100%', maxWidth: 760, minHeight: 58, borderRadius: 17, backgroundColor: tender, alignItems: 'center', justifyContent: 'center' },
  saveText: { fontFamily: font, color: '#FFFFFF', fontSize: 18, fontWeight: '800' },
  notFound: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  title: { fontFamily: font, color: ink, fontSize: 24, fontWeight: '900' },
  backLink: { fontFamily: font, color: tender, marginTop: 16, fontSize: 15 },
});
