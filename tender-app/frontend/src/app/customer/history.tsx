import { Redirect, useRouter } from 'expo-router';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AccountMenu from '@/components/AccountMenu';
import { useAuth } from '@/context/AuthContext';
import { useOrders } from '@/context/OrdersContext';
import { restaurantsMock } from '@/mocks/restaurants.mock';

export default function CustomerHistory() {
  const { user } = useAuth();
  const { orders, cancelOrder } = useOrders();
  const router = useRouter();
  if (!user) return <Redirect href="/auth/login" />;
  if (user.role !== 'customer') return <Redirect href="/account" />;

  return <SafeAreaView style={styles.page}>
    <View style={styles.header}>
      <Pressable onPress={() => router.replace('/customer/home/home')} accessibilityRole="button"><Text style={styles.back}>‹ Back</Text></Pressable>
      <AccountMenu />
    </View>
    <ScrollView contentContainerStyle={styles.content}>
      <Text style={styles.title}>History</Text>
      <Text style={styles.subtitle}>Your orders</Text>
      {orders.length === 0 ? <Text style={styles.empty}>You have no orders yet.</Text> : orders.map((order) => {
        const restaurant = restaurantsMock.find((entry) => entry.id === order.restaurantId);
        return <View key={order.id} style={styles.card}>
          <Text style={styles.restaurant}>{restaurant?.name ?? 'Restaurant'}</Text>
          <Text style={styles.detail}>Order #{order.id} · {new Date(order.createdAt).toLocaleDateString()}</Text>
          <Text style={styles.detail}>${order.total.toFixed(2)} · {order.status.charAt(0).toUpperCase() + order.status.slice(1)}</Text>
          {order.status === 'placed' && <Pressable onPress={() => cancelOrder(order.id)} accessibilityRole="button" style={styles.cancel}><Text style={styles.cancelText}>Cancel order</Text></Pressable>}
        </View>;
      })}
    </ScrollView>
  </SafeAreaView>;
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#FFFAF9' },
  header: { height: 64, maxWidth: 800, width: '100%', alignSelf: 'center', paddingHorizontal: 22, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', zIndex: 10 },
  back: { color: '#B52F4B', fontWeight: '700', fontSize: 16 },
  content: { width: '100%', maxWidth: 600, alignSelf: 'center', padding: 24, paddingBottom: 60 },
  title: { fontSize: 32, fontWeight: '800', color: '#38252A' },
  subtitle: { color: '#79525A', fontSize: 15, marginTop: 6, marginBottom: 24 },
  empty: { color: '#79525A', fontSize: 16, paddingVertical: 25 },
  card: { backgroundColor: '#FFFFFF', borderWidth: 1, borderColor: '#F3D1D7', borderRadius: 16, padding: 18, marginBottom: 12 },
  restaurant: { color: '#38252A', fontFamily: 'LoveLight', fontSize: 28, fontWeight: '400', marginBottom: 8 },
  detail: { color: '#79525A', fontSize: 14, marginTop: 4 },
  cancel: { alignSelf: 'flex-start', marginTop: 14, paddingVertical: 8 },
  cancelText: { color: '#B52F4B', fontWeight: '700' },
});
