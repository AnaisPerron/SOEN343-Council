import { useCallback, useEffect, useRef, useState } from 'react';
import { Redirect, useRouter } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AccountMenu from '@/components/AccountMenu';
import { requestJson } from '@/api/client';
import { useAuth } from '@/context/AuthContext';

type Order = { id: number; restaurantId: number; status: string; dateOrdered: string; totalPrice: number; rejectionReason: string | null; orderLines: { itemName: string; quantity: number; unitPrice: number }[] };
const lifecycle = ['placed', 'accepted', 'preparing', 'ready', 'picked_up', 'delivered'];
const label = (value: string) => value.replace(/_/g, ' ').replace(/^./, (letter) => letter.toUpperCase());

export default function CustomerHistory() {
  const { user, token, ready } = useAuth();
  const router = useRouter();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [cancelling, setCancelling] = useState<number | null>(null);
  const [expanded, setExpanded] = useState<number | null>(null);
  const requestVersion = useRef(0);

  const refresh = useCallback(async () => {
    if (!token) return;
    const version = ++requestVersion.current;
    const data = await requestJson<Order[]>('GET', '/api/orders', undefined, token);
    if (version === requestVersion.current) setOrders(data);
  }, [token]);

  useEffect(() => {
    if (!token || user?.role !== 'customer') return;
    let active = true;
    const load = async () => {
      try { await refresh(); if (active) setMessage(''); }
      catch (error) { if (active) setMessage(error instanceof Error ? error.message : 'Could not load orders.'); }
      finally { if (active) setLoading(false); }
    };
    void load();
    const timer = setInterval(load, 10000);
    return () => { active = false; requestVersion.current++; clearInterval(timer); };
  }, [token, user?.role, refresh]);

  const cancel = async (order: Order) => {
    if (!token || cancelling !== null) return;
    setCancelling(order.id); setMessage('');
    try {
      await requestJson('POST', `/api/orders/${order.id}/cancel`, undefined, token);
      await refresh();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not cancel the order.');
      await refresh().catch(() => {});
    } finally { setCancelling(null); }
  };

  if (!ready) return <View style={styles.center}><ActivityIndicator /></View>;
  if (!user) return <Redirect href="/auth/login" />;
  if (user.role !== 'customer') return <Redirect href="/account" />;
  return <SafeAreaView style={styles.page}>
    <ScrollView contentContainerStyle={styles.content}>
      <View style={styles.header}><Pressable onPress={() => router.replace('/customer/home/home')} accessibilityRole="button"><Text style={styles.back}>‹ Back</Text></Pressable><AccountMenu /></View>
      <Text style={styles.title}>Order history</Text><Text style={styles.subtitle}>Your orders</Text>
      {!!message && <Text accessibilityRole="alert" style={styles.message}>{message}</Text>}
      {loading ? <ActivityIndicator /> : orders.length === 0 ? <Text style={styles.empty}>{message ? 'Orders could not be loaded.' : 'You have no orders yet.'}</Text> : orders.map((order) => {
        const status = order.status.toLowerCase();
        const current = lifecycle.indexOf(status);
        return <View key={order.id} style={styles.card}>
          <Pressable accessibilityRole="button" accessibilityLabel={`View order ${order.id} details`} onPress={() => setExpanded(expanded === order.id ? null : order.id)}>
            <View style={styles.top}><Text style={styles.orderTitle}>Order #{order.id}</Text><Text style={styles.muted}>{new Date(order.dateOrdered).toLocaleDateString()}</Text></View>
            <Text style={styles.muted}>Restaurant #{order.restaurantId}</Text>
            {current >= 0 && <ScrollView horizontal showsHorizontalScrollIndicator={false}><View style={styles.rail}>{lifecycle.map((step, index) => <View key={step} style={styles.step}><View style={[styles.dot, current >= index && styles.dotComplete]} /><Text style={styles.stepLabel}>{label(step)}</Text></View>)}</View></ScrollView>}
            {order.orderLines.map((line, index) => <Text style={styles.item} key={`${order.id}-${index}`}>{line.quantity} × {line.itemName}</Text>)}
            <View style={styles.bottom}><Text style={styles.badge}>{label(status)}</Text><Text style={styles.total}>${Number(order.totalPrice).toFixed(2)}</Text></View>
            {expanded === order.id && <View style={styles.details}>{order.orderLines.map((line, index) => <Text key={index} style={styles.muted}>{line.quantity} × {line.itemName} · ${(line.quantity * Number(line.unitPrice)).toFixed(2)}</Text>)}{order.rejectionReason && <Text style={styles.muted}>Reason: {order.rejectionReason}</Text>}</View>}
          </Pressable>
          {status === 'placed' && <Pressable disabled={cancelling !== null} accessibilityRole="button" onPress={() => void cancel(order)} style={styles.cancel}><Text style={styles.cancelText}>{cancelling === order.id ? 'Cancelling…' : 'Cancel order'}</Text></Pressable>}
        </View>;
      })}
    </ScrollView>
  </SafeAreaView>;
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#F7F7F5' }, center: { flex: 1, justifyContent: 'center' },
  content: { width: '100%', maxWidth: 800, alignSelf: 'center', padding: 22, paddingBottom: 80 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 28 }, back: { color: '#5A626B', fontWeight: '700' },
  title: { color: '#242930', fontSize: 36, fontWeight: '800' }, subtitle: { color: '#69717A', marginTop: 8, marginBottom: 24 },
  empty: { color: '#68737C', padding: 24, backgroundColor: '#FFF' }, message: { color: '#A83B4B', marginBottom: 12 },
  card: { backgroundColor: '#FFF', borderWidth: 1, borderColor: '#DDE2E3', borderRadius: 8, padding: 16, marginBottom: 14 },
  top: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }, orderTitle: { color: '#273039', fontSize: 17, fontWeight: '800' },
  muted: { color: '#68737C', fontSize: 12, marginTop: 6 }, rail: { flexDirection: 'row', minWidth: 380, marginVertical: 18 }, step: { flex: 1, alignItems: 'center' },
  dot: { width: 17, height: 17, borderRadius: 9, borderWidth: 1, borderColor: '#C8CDD0', backgroundColor: '#F3F4F4' }, dotComplete: { backgroundColor: '#3A9B59', borderColor: '#3A9B59' }, stepLabel: { color: '#68737C', fontSize: 9, marginTop: 5 },
  item: { color: '#273039', fontSize: 15, fontWeight: '700', marginBottom: 6 }, bottom: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end', marginTop: 12 },
  badge: { color: '#35414B', fontWeight: '800', backgroundColor: '#EDF0F2', overflow: 'hidden', padding: 6, borderRadius: 5 }, total: { color: '#273039', fontWeight: '800', fontSize: 17 },
  details: { borderTopWidth: 1, borderTopColor: '#E8ECEE', marginTop: 16, paddingTop: 8 }, cancel: { alignSelf: 'flex-end', marginTop: 16, borderWidth: 1, borderColor: '#A83B4B', borderRadius: 6, padding: 10 }, cancelText: { color: '#A83B4B', fontWeight: '800' },
});
