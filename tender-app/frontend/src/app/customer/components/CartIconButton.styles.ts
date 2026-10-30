import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  button: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  darkButton: { backgroundColor: 'rgba(30,25,26,0.66)' },
  lightButton: { backgroundColor: '#FCE7EB' },
  badge: { position: 'absolute', top: -5, right: -6, minWidth: 18, height: 18, borderRadius: 9, paddingHorizontal: 3, backgroundColor: '#D42F50', alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: '#FFFFFF' },
  badgeText: { color: '#FFFFFF', fontSize: 10, fontWeight: '800', lineHeight: 13 },
});
