import { Redirect, useRouter } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AccountMenu from '@/components/AccountMenu';
import CityMap from '@/components/dash/CityMap';
import { useAuth } from '@/context/AuthContext';

export default function RestaurantDashboard() {
  const { user, ready } = useAuth();
  const router = useRouter();
  if (!ready) return <SafeAreaView style={styles.page}><ActivityIndicator color="#D42F50" /></SafeAreaView>;
  if (!user) return <Redirect href="/auth/login" />;
  if (user.role !== 'restaurant') return <Redirect href="/account" />;

  return <SafeAreaView style={styles.page}><ScrollView contentContainerStyle={styles.content}>
    <View style={styles.header}><Text style={styles.eyebrow}></Text><AccountMenu /></View>
    <Text style={styles.title}>{user.name}</Text>
    <Text style={styles.subtitle}>Manage your restaurant</Text>
    <Pressable accessibilityRole="button" style={styles.menuButton} onPress={() => router.push('/restaurant/menu')}>
      <Text style={styles.menuTitle}>Menu</Text><Text style={styles.menuDescription}>Edit and publish your menu →</Text>
    </Pressable>
    <Pressable accessibilityRole="button" style={styles.ordersButton} onPress={() => router.push('/restaurant/orders')}>
      <Text style={styles.ordersTitle}>Orders</Text><Text style={styles.ordersDescription}>Open the order queue →</Text>
    </Pressable>
    <CityMap />
  </ScrollView></SafeAreaView>;
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#FFF' },
  content: { paddingHorizontal: 24, paddingTop: 52 },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', zIndex: 10 },
  eyebrow: { color: '#D42F50', fontSize: 11, fontWeight: '800', letterSpacing: 2 },
  title: { color: '#302629', fontFamily: 'LoveLight', fontSize: 42, fontWeight: '400', marginTop: 16 },
  subtitle: { color: '#887B7E', marginTop: 6, marginBottom: 35 },
  menuButton: { backgroundColor: '#D42F50', borderRadius: 18, padding: 24, marginBottom: 14 },
  menuTitle: { color: '#FFF', fontSize: 23, fontWeight: '900' },
  menuDescription: { color: '#FFF', marginTop: 8 },
  ordersButton: { backgroundColor: '#F7F3F3', borderRadius: 18, padding: 24, marginBottom: 30 },
  ordersTitle: { color: '#302629', fontSize: 23, fontWeight: '900' },
  ordersDescription: { color: '#887B7E', marginTop: 8 },
});
