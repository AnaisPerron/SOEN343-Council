import { useState } from 'react';
import { Redirect, useFocusEffect, useRouter } from 'expo-router';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AccountMenu from '@/components/AccountMenu';
import { useAuth } from '@/context/AuthContext';
import { useOrders } from '@/context/OrdersContext';
import { useCatalog } from '@/context/CatalogContext';
import { formatOrderStatus } from './services/orderLifecycle';

export default function CustomerHistory() {
  const { user } = useAuth();
  const { orders, historyStatus, historyError, refreshOrders, cancelOrder } = useOrders();
  const { restaurants } = useCatalog();
  const router = useRouter();
  const [cancellingId, setCancellingId] = useState<string | null>(null);
  const [cancelErrors, setCancelErrors] = useState<Record<string, string>>({});
  // Reload whenever the page is shown so restaurant status changes appear without signing in again.
  useFocusEffect(refreshOrders);
  if (!user) return <Redirect href="/auth/login" />;
  if (user.role !== 'customer') return <Redirect href="/account" />;

  const cancel = (orderId: string) => {
    if (cancellingId) return;
    setCancellingId(orderId);
    setCancelErrors((current) => ({ ...current, [orderId]: '' }));
    cancelOrder(orderId)
      .catch((error) => setCancelErrors((current) => ({ ...current, [orderId]: error instanceof Error ? error.message : 'Could not cancel the order.' })))
      .finally(() => setCancellingId(null));
  };

  return <SafeAreaView style={styles.page}>
    <View style={styles.header}>
      <Pressable onPress={() => router.replace('/customer/home/home')} accessibilityRole="button"><Text style={styles.back}>‹ Back</Text></Pressable>
      <AccountMenu />
    </View>
    <ScrollView contentContainerStyle={styles.content}>
      <Text style={styles.title}>History</Text>
      <View style={styles.subtitleRow}>
        <Text style={styles.subtitle}>Your orders</Text>
        <Pressable onPress={refreshOrders} accessibilityRole="button"><Text style={styles.refresh}>Refresh</Text></Pressable>
      </View>
      {historyStatus === 'error' && <Text style={styles.error} accessibilityRole="alert">{historyError}</Text>}
      {orders.length === 0 ? <Text style={styles.empty}>{historyStatus === 'loading' ? 'Loading your orders...' : 'You have no orders yet.'}</Text> : orders.map((order) => {
        const restaurantName = order.restaurantName ?? restaurants.find((entry) => entry.id === order.restaurantId)?.name ?? 'Restaurant';
        return <View key={order.id} style={styles.card}>
          <Text style={styles.restaurant}>{restaurantName}</Text>
          <Text style={styles.detail}>Order #{order.id}{order.createdAt ? ` · ${new Date(order.createdAt).toLocaleString()}` : ''}</Text>
          <Text style={styles.detail}>${order.total.toFixed(2)} · {formatOrderStatus(order.status)}</Text>
          {order.items?.map((item, index) => <Text key={index} style={styles.item}>
            {item.quantity} × {item.name}{item.options.length > 0 ? ` (${item.options.join(', ')})` : ''}
          </Text>)}
          {order.status === 'rejected' && !!order.rejectionReason && <Text style={styles.detail}>Reason: {order.rejectionReason}</Text>}
          {order.status === 'placed' && <Pressable onPress={() => cancel(order.id)} disabled={cancellingId !== null} accessibilityRole="button" style={styles.cancel}><Text style={styles.cancelText}>{cancellingId === order.id ? 'Cancelling...' : 'Cancel order'}</Text></Pressable>}
          {!!cancelErrors[order.id] && <Text style={styles.error} accessibilityRole="alert">{cancelErrors[order.id]}</Text>}
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
  subtitleRow: { flexDirection: 'row', alignItems: 'baseline', justifyContent: 'space-between', marginTop: 6, marginBottom: 24 },
  subtitle: { color: '#79525A', fontSize: 15 },
  refresh: { color: '#B52F4B', fontWeight: '700' },
  item: { color: '#38252A', fontSize: 14, marginTop: 6 },
  empty: { color: '#79525A', fontSize: 16, paddingVertical: 25 },
  card: { backgroundColor: '#FFFFFF', borderWidth: 1, borderColor: '#F3D1D7', borderRadius: 16, padding: 18, marginBottom: 12 },
  restaurant: { color: '#38252A', fontFamily: 'LoveLight', fontSize: 28, fontWeight: '400', marginBottom: 8 },
  detail: { color: '#79525A', fontSize: 14, marginTop: 4 },
  cancel: { alignSelf: 'flex-start', marginTop: 14, paddingVertical: 8 },
  cancelText: { color: '#B52F4B', fontWeight: '700' },
  error: { color: '#B52F4B', fontSize: 14, marginTop: 4 },
});
