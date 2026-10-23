import { useRouter } from 'expo-router';
import { Pressable, StyleSheet, Text } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '@/context/AuthContext';

export default function RestaurantDashboard() {
  const { user, signOut } = useAuth();
  const router = useRouter();
  if (!user || user.role !== 'restaurant') {
    router.replace('/auth/login');
    return null;
  }
  return <SafeAreaView style={styles.page}>
    <Text style={styles.eyebrow}>TENDER / RESTAURANT</Text>
    <Text style={styles.title}>{user.name}</Text>
    <Text style={styles.subtitle}>Manage your restaurant</Text>
    <Pressable accessibilityRole="button" style={styles.menuButton} onPress={() => router.push('/restaurant/menu')}>
      <Text style={styles.menuTitle}>Menu</Text><Text style={styles.menuDescription}>Edit and publish your menu →</Text>
    </Pressable>
    <Pressable accessibilityRole="button" accessibilityState={{ disabled: true }} style={styles.ordersButton} disabled>
      <Text style={styles.ordersTitle}>Orders</Text><Text style={styles.ordersDescription}>Coming soon</Text>
    </Pressable>
    <Pressable accessibilityRole="button" onPress={() => { signOut(); router.replace('/auth/login'); }}><Text style={styles.signOut}>Sign out</Text></Pressable>
  </SafeAreaView>;
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#FFF', paddingHorizontal: 24, paddingTop: 52 },
  eyebrow: { color: '#D42F50', fontSize: 11, fontWeight: '800', letterSpacing: 2 },
  title: { color: '#302629', fontSize: 31, fontWeight: '900', marginTop: 16 },
  subtitle: { color: '#887B7E', marginTop: 6, marginBottom: 35 },
  menuButton: { backgroundColor: '#D42F50', borderRadius: 18, padding: 24, marginBottom: 14 },
  menuTitle: { color: '#FFF', fontSize: 23, fontWeight: '900' },
  menuDescription: { color: '#FFF', marginTop: 8 },
  ordersButton: { backgroundColor: '#F7F3F3', borderRadius: 18, padding: 24, marginBottom: 30 },
  ordersTitle: { color: '#887B7E', fontSize: 23, fontWeight: '900' },
  ordersDescription: { color: '#887B7E', marginTop: 8 },
  signOut: { color: '#D42F50', fontWeight: '700' },
});
