import { Pressable, StyleSheet, Text, View } from 'react-native';
import type { OperatorSectionName } from '@/types/operator';

const kinds: { kind: OperatorSectionName; label: string }[] = [
  { kind: 'restaurants', label: 'Restaurants' },
  { kind: 'customers', label: 'Customers' },
  { kind: 'couriers', label: 'Couriers' },
];

export default function MapDirectory({ visible, onToggle }: {
  visible: OperatorSectionName[];
  onToggle: (kind: OperatorSectionName) => void;
}) {
  return (
    <View style={styles.filters}>
      <Text style={styles.title}>Show on map</Text>
      <View style={styles.choices}>
        {kinds.map(({ kind, label }) => (
          <Pressable key={kind} accessibilityRole="checkbox" accessibilityState={{ checked: visible.includes(kind) }}
            onPress={() => onToggle(kind)} style={styles.choice}>
            <Text style={styles.choiceText}>{visible.includes(kind) ? '☑' : '□'} {label}</Text>
          </Pressable>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  filters: { marginBottom: 20 },
  title: { fontFamily: 'Gotham Rounded', fontSize: 14, fontWeight: '700', color: '#38252A', marginBottom: 8 },
  choices: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  choice: { borderWidth: 1, borderColor: '#D9C5CA', borderRadius: 8, paddingHorizontal: 10, paddingVertical: 8 },
  choiceText: { fontFamily: 'Gotham Rounded', color: '#38252A', fontSize: 13, fontWeight: '600' },
});
