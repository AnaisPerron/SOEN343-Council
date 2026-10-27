import { Pressable, StyleSheet, Text, View } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
type Props = { onSelect: (dataUri: string) => void; onError: (message: string) => void; label?: string };

export function CoverImagePicker({ onSelect, onError, label = 'Restaurant cover image' }: Props) {
  const pick = async () => {
    try {
      const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], base64: true, quality: 0.7 });
      if (result.canceled) return;
      const asset = result.assets[0];
      if (!asset.base64) throw new Error('Could not read this image.');
      onSelect(`data:image/jpeg;base64,${asset.base64}`);
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Could not choose an image.');
    }
  };

  return <View style={styles.box}>
    <Text style={styles.label}>{label}</Text>
    <Pressable accessibilityRole="button" onPress={pick} style={styles.button}>
      <Text style={styles.buttonText}>Choose image from device</Text>
    </Pressable>
  </View>;
}

const styles = StyleSheet.create({
  box: { marginTop: 18, padding: 18, borderWidth: 1, borderStyle: 'dashed', borderColor: '#CDBFC1', borderRadius: 12, alignItems: 'center' },
  label: { color: '#302629', fontWeight: '700', marginBottom: 12 },
  button: { backgroundColor: '#D42F50', borderRadius: 9, paddingHorizontal: 16, paddingVertical: 10 },
  buttonText: { color: '#FFF', fontWeight: '700' },
});
