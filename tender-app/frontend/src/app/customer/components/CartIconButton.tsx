import { SymbolView } from 'expo-symbols';
import { Text, TouchableOpacity, View } from 'react-native';
import { styles } from './CartIconButton.styles';

interface CartIconButtonProps {
  count: number;
  onPress: () => void;
  tone?: 'dark' | 'light';
}

export default function CartIconButton({ count, onPress, tone = 'light' }: CartIconButtonProps) {
  return (
    <TouchableOpacity
      style={[styles.button, tone === 'dark' ? styles.darkButton : styles.lightButton]}
      accessibilityRole="button"
      accessibilityLabel={`Cart, ${count} ${count === 1 ? 'item' : 'items'}`}
      onPress={onPress}
    >
      {/* Use the platform's cart glyph while keeping the same button and badge on every screen. */}
      <SymbolView name={{ ios: 'cart', android: 'shopping_cart', web: 'shopping_cart' }} size={22} tintColor={tone === 'dark' ? '#FFFFFF' : '#A92140'} />
      {count > 0 && <View style={styles.badge}><Text style={styles.badgeText}>{count > 99 ? '99+' : count}</Text></View>}
    </TouchableOpacity>
  );
}
