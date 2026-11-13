import { useEffect, useState } from 'react';
import { Redirect } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AccountMenu from '@/components/AccountMenu';
import CityMap from '@/components/dash/CityMap';
import { useAuth } from '@/context/AuthContext';
import { acceptOrder, declineOrder, fetchCourierDuty, goOffDuty, goOnDuty, markOrderDelivered, markOrderPickedUp, type CourierDuty } from '@/api/courierDuty';
import { fetchLedgerHistory, type LedgerHistoryEntry } from '@/api/orderHistory';
import { canGoOffDuty, canGoOnDuty, isOnDuty, stateLabel } from '@/lib/courierDuty';

// Orders can be assigned at any time, so the panel refreshes itself while open
const REFRESH_MS = 15000;
const money = (value: number | null) => value == null ? '' : `$${Number(value).toFixed(2)}`;

export default function CourierDashboard() {
  const { user, token, ready } = useAuth();
  const [duty, setDuty] = useState<CourierDuty | null>(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [message, setMessage] = useState('');
  const [history, setHistory] = useState<LedgerHistoryEntry[]>([]);
  // Going on or off duty changes the courier's map presence, so refresh it immediately.
  const [mapRefreshKey, setMapRefreshKey] = useState(0);
  const order = duty?.assignedOrder;

  useEffect(() => {
    if (!token || user?.role !== 'courier') return;
    let active = true;
    const refresh = async () => {
      try {
        const loaded = await fetchCourierDuty(token);
        if (active) setDuty(loaded);
        const previous = await fetchLedgerHistory(token);
        if (active) setHistory(previous);
      } catch (error) { if (active) setMessage(error instanceof Error ? error.message : 'Could not load your status.'); }
      finally { if (active) setLoading(false); }
    };
    void refresh();
    const timer = setInterval(refresh, REFRESH_MS);
    return () => { active = false; clearInterval(timer); };
  }, [token, user?.role]);

  const toggle = async () => {
    if (!token || !duty || updating) return;
    setUpdating(true); setMessage('');
    try { setDuty(await (isOnDuty(duty.status) ? goOffDuty(token) : goOnDuty(token))); setMapRefreshKey((key) => key + 1); }
    catch (error) { setMessage(error instanceof Error ? error.message : 'Could not change your duty status.'); }
    finally { setUpdating(false); }
  };

  const performOrderAction = async (action: (token: string, orderId: number) => Promise<CourierDuty>, failure: string) => {
    if (!token || !order || updating) return;
    setUpdating(true);
    setMessage('');
    try {
      setDuty(await action(token, order.orderId));
      setMapRefreshKey((key) => key + 1);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : failure);
    } finally {
      setUpdating(false);
    }
  };

  const confirmDelivery = async (orderId: number) => {
    if (!token || updating) return;
    setUpdating(true);
    setMessage('');
    try {
      setDuty(await markOrderDelivered(token, orderId));
      setMapRefreshKey((key) => key + 1);
      setMessage(`Order #${orderId} delivered.`);
      try {
        setHistory(await fetchLedgerHistory(token));
      } catch {
        setMessage(`Order #${orderId} delivered, but the ledger history could not be refreshed.`);
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not confirm delivery.');
    } finally { setUpdating(false); }
  };

  if (!ready) return <SafeAreaView style={styles.page}><ActivityIndicator color="#D42F50" /></SafeAreaView>;
  if (!user) return <Redirect href="/auth/login" />;
  if (user.role !== 'courier') return <Redirect href="/account" />;

  const onDuty = duty != null && isOnDuty(duty.status);
  const toggleEnabled = duty != null && !updating && (onDuty ? canGoOffDuty(duty.status) : canGoOnDuty(duty.status));

  return <SafeAreaView style={styles.page}>
    <View style={styles.header}><Text style={styles.brand}>Tender</Text><AccountMenu /></View>
    <ScrollView contentContainerStyle={styles.content}>
      <Text style={styles.title}>{user.name}</Text>
      <Text style={styles.subtitle}>Courier dashboard</Text>

      {loading ? <ActivityIndicator color="#D42F50" /> : duty && <>
        <View style={[styles.stateCard, onDuty && styles.stateCardOn]}>
          <Text style={[styles.stateEyebrow, onDuty && styles.onText]}>CURRENT STATE</Text>
          <Text style={[styles.stateValue, onDuty && styles.onText]}>
            {duty.status === 'idle' && order?.status === 'ready' ? 'On duty · order offered' : stateLabel(duty.status)}
          </Text>
        </View>

        <Pressable accessibilityRole="button" accessibilityState={{ disabled: !toggleEnabled }} disabled={!toggleEnabled} onPress={toggle}
          style={[styles.toggle, onDuty ? styles.toggleOff : styles.toggleOn, !toggleEnabled && styles.toggleDisabled]}>
          <Text style={[styles.toggleText, onDuty && styles.toggleOffText]}>{updating ? 'Updating…' : onDuty ? 'Go off duty' : 'Go on duty'}</Text>
        </Pressable>
        {onDuty && !canGoOffDuty(duty.status) && <Text style={styles.hint}>You can go off duty once your current delivery is done.</Text>}

        <Text style={styles.sectionHeading}>{duty.status === 'idle' && order?.status === 'ready' ? 'Delivery offer' : 'Current delivery'}</Text>
        {order ? <View style={styles.orderCard}>
          <View style={styles.orderRow}><Text style={styles.orderTitle}>Order #{order.orderId}</Text><Text style={styles.orderStatus}>{order.status.replace('_', ' ')}</Text></View>
          {!!order.restaurantName && <Text style={styles.orderLine}>Pick up at {order.restaurantName}</Text>}
          {!!order.restaurantAddress && <Text style={styles.orderMuted}>{order.restaurantAddress}</Text>}
          {!!order.deliveryAddress && <Text style={styles.orderLine}>Deliver to {order.deliveryAddress}</Text>}
          {order.totalPrice != null && <Text style={styles.orderMuted}>Order total {money(order.totalPrice)}</Text>}
          {duty.status === 'idle' && order.status === 'ready' && <View style={styles.offerActions}>
            <Pressable accessibilityRole="button" disabled={updating} onPress={() => void performOrderAction(acceptOrder, 'Could not accept this order.')} style={[styles.deliveredButton, updating && styles.toggleDisabled]}>
              <Text style={styles.deliveredButtonText}>{updating ? 'Updating…' : 'Accept order'}</Text>
            </Pressable>
            <Pressable accessibilityRole="button" disabled={updating} onPress={() => void performOrderAction(declineOrder, 'Could not decline this order.')} style={[styles.declineButton, updating && styles.toggleDisabled]}>
              <Text style={styles.declineButtonText}>Decline and go off duty</Text>
            </Pressable>
          </View>}
          {duty.status === 'assigned' && order.status === 'ready' && <Pressable
            accessibilityRole="button"
            accessibilityState={{ disabled: updating }}
            disabled={updating}
            onPress={() => void performOrderAction(markOrderPickedUp, 'Could not confirm pickup.')}
            style={[styles.deliveredButton, updating && styles.toggleDisabled]}>
            <Text style={styles.deliveredButtonText}>{updating ? 'Updating…' : 'Confirm pickup'}</Text>
          </Pressable>}
          {duty.status === 'carrying' && order.status === 'picked_up' && <Pressable
            accessibilityRole="button"
            accessibilityState={{ disabled: updating }}
            disabled={updating}
            onPress={() => void confirmDelivery(order.orderId)}
            style={[styles.deliveredButton, updating && styles.toggleDisabled]}>
            <Text style={styles.deliveredButtonText}>{updating ? 'Updating…' : 'Confirm delivery'}</Text>
          </Pressable>}
        </View> : <Text style={styles.empty}>{onDuty ? 'No order is currently offered. You will receive the oldest ready order nearby.' : 'Go on duty to receive delivery offers.'}</Text>}
      </>}
      <Text style={styles.sectionHeading}>Delivered orders</Text>
      {history.length === 0 ? <Text style={styles.empty}>No completed deliveries yet.</Text> : history.map((entry) =>
        <View key={entry.orderId} style={styles.orderCard}>
          <Text style={styles.orderTitle}>Order #{entry.orderId}</Text>
          <Text style={styles.orderMuted}>{entry.status.replaceAll('_', ' ').toLowerCase()} · {entry.deliveredAt ? new Date(entry.deliveredAt).toLocaleString() : 'Delivery time unavailable'}</Text>
          <Text style={styles.orderLine}>Total charge {money(entry.totalCharge)}</Text>
        </View>)}
      {!!message && <Text style={styles.message}>{message}</Text>}
      <View style={styles.map}><CityMap refreshKey={mapRefreshKey} /></View>
    </ScrollView>
  </SafeAreaView>;
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#FFFAF9' },
  header: { height: 70, width: '100%', maxWidth: 1000, alignSelf: 'center', paddingHorizontal: 24, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', zIndex: 10 },
  brand: { color: '#D42F50', fontSize: 25, fontWeight: '900' },
  content: { padding: 24, width: '100%', maxWidth: 1000, alignSelf: 'center' },
  title: { color: '#302629', fontFamily: 'LoveLight', fontSize: 42, fontWeight: '400' },
  subtitle: { color: '#887B7E', marginTop: 6, marginBottom: 28 },
  stateCard: { backgroundColor: '#F7F3F3', borderRadius: 18, padding: 24 },
  stateCardOn: { backgroundColor: '#D42F50' },
  stateEyebrow: { color: '#887B7E', fontSize: 11, fontWeight: '800', letterSpacing: 2 },
  stateValue: { color: '#302629', fontSize: 23, fontWeight: '900', marginTop: 8 },
  onText: { color: '#FFF' },
  toggle: { borderRadius: 14, alignItems: 'center', padding: 16, marginTop: 14 },
  toggleOn: { backgroundColor: '#D42F50' },
  toggleOff: { borderWidth: 1, borderColor: '#D42F50', backgroundColor: '#FFF' },
  toggleDisabled: { opacity: 0.4 },
  toggleText: { color: '#FFF', fontWeight: '800' },
  toggleOffText: { color: '#D42F50' },
  hint: { color: '#887B7E', fontSize: 12, marginTop: 8, textAlign: 'center' },
  sectionHeading: { color: '#302629', fontSize: 18, fontWeight: '800', marginTop: 32, marginBottom: 12 },
  orderCard: { backgroundColor: '#FFF', borderWidth: 1, borderColor: '#EDE6E6', borderRadius: 18, padding: 20 },
  deliveredButton: { backgroundColor: '#D42F50', borderRadius: 14, alignItems: 'center', padding: 16, marginTop: 16 },
  deliveredButtonText: { color: '#FFF', fontWeight: '800' },
  offerActions: { gap: 8 },
  declineButton: { borderWidth: 1, borderColor: '#887B7E', borderRadius: 14, alignItems: 'center', padding: 14, marginTop: 8 },
  declineButtonText: { color: '#62585A', fontWeight: '800' },
  orderRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  orderTitle: { color: '#302629', fontSize: 18, fontWeight: '800' },
  orderStatus: { color: '#D42F50', fontWeight: '800', textTransform: 'capitalize' },
  orderLine: { color: '#302629', marginTop: 10 },
  orderMuted: { color: '#887B7E', marginTop: 4 },
  empty: { color: '#887B7E' },
  message: { color: '#302629', marginTop: 16 },
  map: { marginTop: 32 },
});
