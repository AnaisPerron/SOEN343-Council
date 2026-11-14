import { useEffect, useState } from 'react';
import { Redirect, useFocusEffect, useRouter } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AccountMenu from '@/components/AccountMenu';
import { useAuth } from '@/context/AuthContext';
import { fetchCustomerOrderSummary, type OrderSummary } from '@/api/orderSummary';
import { useOrders } from '@/context/OrdersContext';
import { useCatalog } from '@/context/CatalogContext';
import { formatOrderStatus, type CustomerOrderStatus } from './services/orderLifecycle';

const lifecycle: CustomerOrderStatus[] = ['placed', 'accepted', 'preparing', 'ready', 'picked_up', 'delivered'];

export default function CustomerHistory() {
  const { user, ready, token } = useAuth();
  const { orders, historyStatus, historyError, refreshOrders, cancelOrder } = useOrders();
  const { restaurants } = useCatalog();
  const router = useRouter();
  const [cancellingId, setCancellingId] = useState<string | null>(null);
  const [cancelErrors, setCancelErrors] = useState<Record<string, string>>({});
  const [expanded, setExpanded] = useState<string | null>(null);
  const [summaries, setSummaries] = useState<Record<string, OrderSummary>>({});
  const [summaryLoading, setSummaryLoading] = useState<string | null>(null);
  const [summaryErrors, setSummaryErrors] = useState<Record<string, string>>({});
  // Reload whenever the page is shown, then poll so restaurant status changes appear while it stays open.
  useFocusEffect(refreshOrders);
  useEffect(() => {
    const timer = setInterval(refreshOrders, 10000);
    return () => clearInterval(timer);
  }, [refreshOrders]);
  if (!ready) return <View style={styles.center}><ActivityIndicator /></View>;
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

  const toggleDetails = (orderId: string) => {
    if (expanded === orderId) { setExpanded(null); return; }
    setExpanded(orderId);
    if (!token || summaryLoading === orderId) return;
    setSummaryLoading(orderId);
    setSummaryErrors((current) => ({ ...current, [orderId]: '' }));
    fetchCustomerOrderSummary(orderId, token)
      .then((summary) => setSummaries((current) => ({ ...current, [orderId]: summary })))
      .catch((error) => setSummaryErrors((current) => ({ ...current, [orderId]: error instanceof Error ? error.message : 'Could not load order summary.' })))
      .finally(() => setSummaryLoading((current) => current === orderId ? null : current));
  };

  return <SafeAreaView style={styles.page}>
    <ScrollView contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <Pressable onPress={() => router.replace('/customer/home/home')} accessibilityRole="button"><Text style={styles.back}>‹ Back</Text></Pressable>
        <AccountMenu />
      </View>
      <Text style={styles.title}>Order history</Text>
      <View style={styles.subtitleRow}>
        <Text style={styles.subtitle}>Your orders</Text>
        <Pressable onPress={refreshOrders} accessibilityRole="button"><Text style={styles.refresh}>Refresh</Text></Pressable>
      </View>
      {historyStatus === 'error' && <Text style={styles.message} accessibilityRole="alert">{historyError}</Text>}
      {orders.length === 0
        ? historyStatus === 'loading' ? <ActivityIndicator /> : <Text style={styles.empty}>{historyStatus === 'error' ? 'Orders could not be loaded.' : 'You have no orders yet.'}</Text>
        : orders.map((order) => {
          const restaurantName = order.restaurantName ?? restaurants.find((entry) => entry.id === order.restaurantId)?.name ?? 'Restaurant';
          const current = lifecycle.indexOf(order.status);
          return <View key={order.id} style={styles.card}>
            <Pressable accessibilityRole="button" accessibilityLabel={`View order ${order.id} details`} onPress={() => toggleDetails(order.id)}>
              <View style={styles.top}><Text style={styles.orderTitle}>Order #{order.id}</Text>{!!order.createdAt && <Text style={styles.muted}>{new Date(order.createdAt).toLocaleDateString()}</Text>}</View>
              <Text style={styles.restaurant}>{restaurantName}</Text>
              {current >= 0 && <ScrollView horizontal showsHorizontalScrollIndicator={false}><View style={styles.rail}>{lifecycle.map((step, index) => <View key={step} style={styles.step}><View style={[styles.dot, current >= index && styles.dotComplete]} /><Text style={styles.stepLabel}>{formatOrderStatus(step)}</Text></View>)}</View></ScrollView>}
              {order.items?.map((item, index) => <Text key={index} style={styles.item}>
                {item.quantity} × {item.name}{item.options.length > 0 ? ` (${item.options.join(', ')})` : ''}
              </Text>)}
              <View style={styles.bottom}><Text style={styles.badge}>{formatOrderStatus(order.status)}</Text><Text style={styles.total}>${Number(summaries[order.id]?.totalCharge ?? order.total).toFixed(2)}</Text></View>
              {expanded === order.id && <View style={styles.details}>
                {!!order.createdAt && <Text style={styles.muted}>Placed {new Date(order.createdAt).toLocaleString()}</Text>}
                {summaryLoading === order.id && <ActivityIndicator accessibilityLabel="Loading order summary" />}
                {!!summaryErrors[order.id] && <Text style={styles.message} accessibilityRole="alert">{summaryErrors[order.id]}</Text>}
                {summaries[order.id] ? <>
                  {!!summaries[order.id].cancelledAt && <Text style={styles.muted}>Cancelled {new Date(summaries[order.id].cancelledAt ?? '').toLocaleString()}</Text>}
                  {!!summaries[order.id].finishedAt && <Text style={styles.muted}>Finished {new Date(summaries[order.id].finishedAt ?? '').toLocaleString()}</Text>}
                  {summaries[order.id].orderItems.map((item, index) => <Text key={index} style={styles.muted}>{item.quantity} × {item.name} · ${Number(item.lineTotal).toFixed(2)}</Text>)}
                  {summaries[order.id].itemSubtotal != null && <Text style={styles.muted}>Items subtotal: ${Number(summaries[order.id].itemSubtotal).toFixed(2)}</Text>}
                  {summaries[order.id].charges.map((charge, index) => <Text key={index} style={styles.muted}>{charge.chargeType}: ${Number(charge.amount).toFixed(2)}</Text>)}
                  {summaries[order.id].totalCharge != null && <Text style={styles.total}>Total charged: ${Number(summaries[order.id].totalCharge).toFixed(2)}</Text>}
                </> : order.items?.map((item, index) => <Text key={index} style={styles.muted}>{item.quantity} × {item.name} · ${(item.quantity * item.unitPrice).toFixed(2)}</Text>)}
                {!!order.rejectionReason && <Text style={styles.muted}>Reason: {order.rejectionReason}</Text>}
              </View>}
            </Pressable>
            <Pressable accessibilityRole="button" accessibilityLabel={`View summary for order ${order.id}`} onPress={() => router.push({ pathname: '/customer/order-summary/[id]', params: { id: order.id } })} style={styles.cancel}><Text style={styles.cancelText}>View summary</Text></Pressable>
            {order.status === 'placed' && <Pressable onPress={() => cancel(order.id)} disabled={cancellingId !== null} accessibilityRole="button" style={styles.cancel}><Text style={styles.cancelText}>{cancellingId === order.id ? 'Cancelling…' : 'Cancel order'}</Text></Pressable>}
            {!!cancelErrors[order.id] && <Text style={styles.message} accessibilityRole="alert">{cancelErrors[order.id]}</Text>}
          </View>;
        })}
    </ScrollView>
  </SafeAreaView>;
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#F7F7F5' }, center: { flex: 1, justifyContent: 'center' },
  content: { width: '100%', maxWidth: 800, alignSelf: 'center', padding: 22, paddingBottom: 80 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 28, zIndex: 10 }, back: { color: '#5A626B', fontWeight: '700' },
  title: { color: '#242930', fontSize: 36, fontWeight: '800' },
  subtitleRow: { flexDirection: 'row', alignItems: 'baseline', justifyContent: 'space-between', marginTop: 8, marginBottom: 24 },
  subtitle: { color: '#69717A' }, refresh: { color: '#5A626B', fontWeight: '700' },
  empty: { color: '#68737C', padding: 24, backgroundColor: '#FFF' }, message: { color: '#A83B4B', marginTop: 8, marginBottom: 12 },
  card: { backgroundColor: '#FFF', borderWidth: 1, borderColor: '#DDE2E3', borderRadius: 8, padding: 16, marginBottom: 14 },
  top: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }, orderTitle: { color: '#273039', fontSize: 17, fontWeight: '800' },
  restaurant: { color: '#38414A', fontSize: 14, fontWeight: '600', marginTop: 4 },
  muted: { color: '#68737C', fontSize: 12, marginTop: 6 }, rail: { flexDirection: 'row', minWidth: 380, marginVertical: 18 }, step: { flex: 1, alignItems: 'center' },
  dot: { width: 17, height: 17, borderRadius: 9, borderWidth: 1, borderColor: '#C8CDD0', backgroundColor: '#F3F4F4' }, dotComplete: { backgroundColor: '#3A9B59', borderColor: '#3A9B59' }, stepLabel: { color: '#68737C', fontSize: 9, marginTop: 5 },
  item: { color: '#273039', fontSize: 15, fontWeight: '700', marginBottom: 6 }, bottom: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end', marginTop: 12 },
  badge: { color: '#35414B', fontWeight: '800', backgroundColor: '#EDF0F2', overflow: 'hidden', padding: 6, borderRadius: 5 }, total: { color: '#273039', fontWeight: '800', fontSize: 17 },
  details: { borderTopWidth: 1, borderTopColor: '#E8ECEE', marginTop: 16, paddingTop: 8 }, cancel: { alignSelf: 'flex-end', marginTop: 16, borderWidth: 1, borderColor: '#A83B4B', borderRadius: 6, padding: 10 }, cancelText: { color: '#A83B4B', fontWeight: '800' },
});
