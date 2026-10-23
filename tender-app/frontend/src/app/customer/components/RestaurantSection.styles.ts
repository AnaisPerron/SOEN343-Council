import { StyleSheet } from 'react-native';

const roundedFont = 'Gotham Rounded';

export const styles = StyleSheet.create({
  section: { marginTop: 23 },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 13,
  },
  sectionTitles: { flex: 1 },
  sectionTitle: {
    fontFamily: roundedFont,
    color: '#2C1E22',
    fontSize: 19,
    fontWeight: '800',
    letterSpacing: -0.5,
  },
  sectionNote: { fontFamily: roundedFont, color: '#9B858A', fontSize: 11, marginTop: 3 },
  arrows: { flexDirection: 'row', gap: 7 },
  arrow: {
    width: 33,
    height: 33,
    borderWidth: 1,
    borderColor: '#F1D9DC',
    borderRadius: 17,
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
  },
  arrowText: {
    fontFamily: roundedFont,
    color: '#D83D55',
    fontSize: 24,
    lineHeight: 26,
    marginTop: -2,
  },
  cardRow: { gap: 13, paddingBottom: 2, paddingRight: 18 },
  emptySearch: {
    fontFamily: roundedFont,
    color: '#8E777C',
    fontSize: 13,
    paddingVertical: 24,
    textAlign: 'center',
  },
});
