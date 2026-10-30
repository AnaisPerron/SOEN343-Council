import { useRef, useState } from 'react';
import { useRouter } from 'expo-router';
import { Modal, Pressable, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { useAuth } from '@/context/AuthContext';

export default function AccountMenu() {
  const { user, signOut } = useAuth();
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [menuPosition, setMenuPosition] = useState({ top: 44, right: 0 });
  const anchor = useRef<React.ComponentRef<typeof View>>(null);
  const { width: screenWidth } = useWindowDimensions();
  if (!user) return null;

  const toggle = () => {
    if (open) { setOpen(false); return; }
    anchor.current?.measureInWindow((x, y, width, height) => {
      setMenuPosition({ top: y + height + 6, right: Math.max(8, screenWidth - x - width) });
      setOpen(true);
    });
  };

  const navigate = (path: '/profile' | '/customer/history') => {
    setOpen(false);
    router.push(path);
  };

  return <View ref={anchor} style={styles.container}>
    <Pressable
      style={styles.circle}
      onPress={toggle}
      accessibilityRole="button"
      accessibilityLabel="Account menu"
      accessibilityState={{ expanded: open }}
    ><Text style={styles.initial}>{user.name.trim().charAt(0).toLocaleUpperCase() || '?'}</Text></Pressable>
    <Modal visible={open} transparent animationType="none" onRequestClose={() => setOpen(false)}>
      <View style={styles.overlay}>
        <Pressable style={StyleSheet.absoluteFill} onPress={() => setOpen(false)} accessibilityLabel="Close account menu" />
        <View style={[styles.menu, menuPosition]}>
          <Text style={[styles.name, (user.role === 'restaurant' || user.role === 'customer') && styles.restaurantName]} numberOfLines={1}>{user.name}</Text>
          <Pressable style={styles.option} onPress={() => navigate('/profile')} accessibilityRole="button"><Text style={styles.optionText}>Profile</Text></Pressable>
          {user.role === 'customer' && <Pressable style={styles.option} onPress={() => navigate('/customer/history')} accessibilityRole="button"><Text style={styles.optionText}>History</Text></Pressable>}
          <Pressable style={styles.option} onPress={() => { setOpen(false); signOut(); router.replace('/auth/login'); }} accessibilityRole="button"><Text style={styles.optionText}>Sign Out</Text></Pressable>
        </View>
      </View>
    </Modal>
  </View>;
}

const styles = StyleSheet.create({
  container: { position: 'relative', zIndex: 20 },
  overlay: { flex: 1 },
  circle: { width: 38, height: 38, borderRadius: 19, backgroundColor: '#FFE0E5', alignItems: 'center', justifyContent: 'center' },
  initial: { color: '#B52F4B', fontFamily: 'LoveLight', fontWeight: '400', fontSize: 28 },
  restaurantName: { fontFamily: 'LoveLight', fontWeight: '400', fontSize: 25 },
  menu: { position: 'absolute', width: 190, backgroundColor: '#FFFFFF', borderRadius: 14, borderWidth: 1, borderColor: '#F3D1D7', paddingVertical: 6, shadowColor: '#50232D', shadowOpacity: 0.16, shadowRadius: 12, elevation: 8 },
  name: { color: '#715158', fontWeight: '700', paddingHorizontal: 15, paddingVertical: 9 },
  option: { paddingHorizontal: 15, paddingVertical: 12 },
  optionText: { color: '#38252A', fontSize: 15, fontWeight: '600' },
});
