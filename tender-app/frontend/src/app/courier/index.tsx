import { useEffect, useRef, useState } from 'react';
import { Redirect } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AccountMenu from '@/components/AccountMenu';
import CityMap from '@/components/dash/CityMap';
import type { MapData } from '@/types/dash';
import { queueBand, queueBandColor } from '@/lib/queueBands';
import { styles } from './couriers.styles';
import { useAuth } from '@/context/AuthContext';
import { ApiError } from '@/api/client';
import { acceptCourierOffer, declineOrder, fetchCourierDuty, goOffDuty, goOnDuty, markOrderDelivered, markOrderPickedUp, type CourierDuty } from '@/api/courierDuty';
import { fetchLedgerHistory, type LedgerHistoryEntry } from '@/api/orderHistory';
import { canGoOffDuty, canGoOnDuty, getOrderActionAvailability, isOnDuty, stateLabel } from '@/lib/courierDuty';
import LoadingScreen from '@/components/LoadingScreen';

// Orders can be assigned at any time, so the panel refreshes itself while open
const REFRESH_MS = 15000;
const money = (value: number) => `$${value.toFixed(2)}`;

export default function CourierDashboard() {
  const { user, token, ready, signOut } = useAuth();
  const [duty, setDuty] = useState<CourierDuty | null>(null);
  const [loading, setLoading] = useState(true);
  const [switching, setSwitching] = useState(false);
  const [message, setMessage] = useState('');
  const [history, setHistory] = useState<LedgerHistoryEntry[]>([]);
  const [accepting, setAccepting] = useState(false);
  const [updatingOrder, setUpdatingOrder] = useState(false);
  const mutating = useRef(false);
  const requestId = useRef(0);
  const [mapRefreshKey, setMapRefreshKey] = useState(0);
  const [mapData, setMapData] = useState<MapData | null>(null);

  useEffect(() => {
    if (!token || user?.role !== 'courier') return;
    let active = true;
    const refresh = async () => {
      if (mutating.current) return;
      const version = ++requestId.current;
      try {
        const loaded = await fetchCourierDuty(token);
        if (active && version === requestId.current) { setDuty(loaded); setMessage(''); }
        try {
          const previous = await fetchLedgerHistory(token);
          if (active && version === requestId.current) setHistory(previous);
        } catch (error) {
          if (active && version === requestId.current) {
            if (error instanceof ApiError && error.status === 401) signOut();
            else setMessage(error instanceof Error ? error.message : 'Could not load delivery history.');
          }
        }
      } catch (error) {
        if (active && version === requestId.current) {
          if (error instanceof ApiError && error.status === 401) signOut();
          else setMessage(error instanceof Error ? error.message : 'Could not load your status.');
        }
      }
      finally { if (active && version === requestId.current) setLoading(false); }
    };
    void refresh();
    const timer = setInterval(refresh, REFRESH_MS);
    return () => { active = false; requestId.current += 1; clearInterval(timer); };
  }, [token, user?.role, signOut]);

  const toggle = async () => {
    if (!token || !duty || mutating.current) return;
    mutating.current = true;
    const version = ++requestId.current;
    setSwitching(true); setMessage('');
    try {
      const next = await (isOnDuty(duty.status) ? goOffDuty(token) : goOnDuty(token));
      if (version !== requestId.current) return;
      setDuty(next); setMapRefreshKey((key) => key + 1);
    } catch (error) {
      if (version !== requestId.current) return;
      if (error instanceof ApiError && error.status === 401) signOut();
      else setMessage(error instanceof Error ? error.message : 'Could not change your duty status.');
    } finally { mutating.current = false; if (version === requestId.current) setSwitching(false); }
  };

  const acceptOffer = async () => {
    const offer = duty?.assignedOrder;
    if (!token || !offer || offer.assignmentStatus !== 'offered' || mutating.current) return;
    mutating.current = true;
    const version = ++requestId.current;
    setAccepting(true); setMessage('');
    try {
      const accepted = await acceptCourierOffer(offer.assignmentId, token);
      if (version !== requestId.current) return;
      setDuty((current) => current ? { ...current, assignedOrder: accepted } : current);
      setMapRefreshKey((key) => key + 1);
    } catch (error) {
      if (version !== requestId.current) return;
      if (error instanceof ApiError && error.status === 401) signOut();
      else {
        if (error instanceof ApiError && (error.status === 409 || error.status === 404)) {
          try {
            const refreshed = await fetchCourierDuty(token);
            if (version !== requestId.current) return;
            setDuty(refreshed);
          } catch (refreshError) {
            if (refreshError instanceof ApiError && refreshError.status === 401) signOut();
          }
        }
        setMessage(error instanceof Error ? error.message : 'Could not accept the offer.');
      }
    } finally { mutating.current = false; if (version === requestId.current) setAccepting(false); }
  };

  const performOrderAction = async (
    action: (token: string, orderId: number) => Promise<CourierDuty>,
    refreshHistory = false,
  ) => {
    const current = duty?.assignedOrder;
    if (!token || !current || mutating.current) return;
    mutating.current = true;
    const version = ++requestId.current;
    setUpdatingOrder(true); setMessage('');
    try {
      const next = await action(token, current.orderId);
      if (version !== requestId.current) return;
      setDuty(next); setMapRefreshKey((key) => key + 1);
      if (refreshHistory) {
        try {
          setHistory(await fetchLedgerHistory(token));
          if (version === requestId.current) setMessage(`Order #${current.orderId} delivered.`);
        } catch (error) {
          if (version === requestId.current) {
            if (error instanceof ApiError && error.status === 401) signOut();
            else setMessage(`Order #${current.orderId} delivered, but the ledger history could not be refreshed.`);
          }
        }
      }
    } catch (error) {
      if (version !== requestId.current) return;
      if (error instanceof ApiError && error.status === 401) signOut();
      else {
        if (error instanceof ApiError && (error.status === 404 || error.status === 409)) {
          try {
            const next = await fetchCourierDuty(token);
            if (version !== requestId.current) return;
            setDuty(next);
          } catch (refreshError) {
            if (refreshError instanceof ApiError && refreshError.status === 401) signOut();
          }
        }
        setMessage(error instanceof Error ? error.message : 'Could not update your delivery.');
      }
    } finally { mutating.current = false; if (version === requestId.current) setUpdatingOrder(false); }
  };

  if (!ready) return <LoadingScreen />;
  if (!user) return <Redirect href="/auth/login" />;
  if (user.role !== 'courier') return <Redirect href="/account" />;

  const onDuty = duty != null && isOnDuty(duty.status);
  const order = duty?.assignedOrder;
  const busy = switching || accepting || updatingOrder;
  const pickupRestaurant = mapData?.restaurants.find((restaurant) => restaurant.name === order?.restaurantName);
  const availability = getOrderActionAvailability(duty?.status, order?.status, order?.assignmentStatus);
  const pickupEnabled = !busy && availability.pickupEnabled;
  const deliveryEnabled = !busy && availability.deliveryEnabled;
  const offered = duty?.status === 'assigned' && order?.assignmentStatus === 'offered' && order.status === 'ready';
  const toggleEnabled = duty != null && !busy && (onDuty ? canGoOffDuty(duty.status) || offered : canGoOnDuty(duty.status));

  return <SafeAreaView style={styles.page}>
    <View style={styles.header}><Text style={styles.brand}>Tender</Text><AccountMenu /></View>
    <ScrollView contentContainerStyle={styles.content}>
      <Text style={styles.title}>{user.name}</Text>
      <Text style={styles.subtitle}>Courier dashboard</Text>

      {loading ? <ActivityIndicator color="#D42F50" /> : duty && <>
        <View style={[styles.stateCard, onDuty && styles.stateCardOn]}>
          <Text style={[styles.stateEyebrow, onDuty && styles.onText]}>CURRENT STATE</Text>
          <Text style={[styles.stateValue, onDuty && styles.onText]}>{stateLabel(duty.status)}</Text>
        </View>

        <Pressable accessibilityRole="button" accessibilityState={{ disabled: !toggleEnabled }} disabled={!toggleEnabled} onPress={toggle}
          style={[styles.toggle, onDuty ? styles.toggleOff : styles.toggleOn, !toggleEnabled && styles.toggleDisabled]}>
          <Text style={[styles.toggleText, onDuty && styles.toggleOffText]}>{switching ? 'Updating…' : onDuty ? 'Go off duty' : 'Go on duty'}</Text>
        </Pressable>
        {onDuty && !canGoOffDuty(duty.status) && !offered && <Text style={styles.hint}>You can go off duty once your current delivery is done.</Text>}

        <Text style={styles.sectionHeading}>{order?.assignmentStatus === 'offered' ? 'Delivery offer' : 'Assigned order'}</Text>
        {order ? <View style={styles.orderCard}>
          <View style={styles.orderRow}><Text style={styles.orderTitle}>Order #{order.orderId}</Text><Text style={styles.orderStatus}>{order.status.replace('_', ' ')}</Text></View>
          {!!order.restaurantName && <Text style={styles.orderLine}>Pick up at {order.restaurantName}</Text>}
          {pickupRestaurant && <View style={styles.queueRow}><View style={[styles.queueDot, { backgroundColor: queueBandColor[queueBand(pickupRestaurant.queueCount, pickupRestaurant.status)] }]} /><Text style={styles.orderMuted}>(Queue Length: {pickupRestaurant.queueCount})</Text></View>}
          <Text style={styles.orderMuted}>{order.restaurantAddress || 'Pickup address unavailable'}</Text>
          <Text style={styles.orderLine}>Deliver to {order.deliveryAddress || 'address unavailable'}</Text>
          <Text style={styles.orderMuted}>{order.assignmentStatus === 'offered' ? 'Waiting for your acceptance' : 'Offer accepted'}</Text>
          {order.assignmentStatus === 'offered' && duty.status === 'assigned' && order.status === 'ready' && (
            <Pressable accessibilityRole="button" accessibilityState={{ disabled: busy }}
              disabled={busy} onPress={() => void acceptOffer()}
              style={[styles.toggle, styles.toggleOn, busy && styles.toggleDisabled]}>
              <Text style={styles.toggleText}>{accepting ? 'Accepting…' : 'Accept offer'}</Text>
            </Pressable>
          )}
          {offered && <Pressable accessibilityRole="button" disabled={busy} accessibilityState={{ disabled: busy }}
            onPress={() => void performOrderAction(declineOrder)} style={[styles.toggle, styles.toggleOff, busy && styles.toggleDisabled]}>
            <Text style={styles.toggleOffText}>Decline and go off duty</Text>
          </Pressable>}
          <View style={styles.actionRow}>
            <Pressable accessibilityRole="button" disabled={!pickupEnabled} accessibilityState={{ disabled: !pickupEnabled }}
              accessibilityHint={availability.pickupHint}
              onPress={() => void performOrderAction(markOrderPickedUp)} style={[styles.actionButton, !pickupEnabled && styles.toggleDisabled]}>
              <Text style={styles.deliveredButtonText}>{updatingOrder ? 'Updating…' : 'Confirm pickup'}</Text>
            </Pressable>
            <Pressable accessibilityRole="button" disabled={!deliveryEnabled} accessibilityState={{ disabled: !deliveryEnabled }}
              accessibilityHint={availability.deliveryHint}
              onPress={() => void performOrderAction(markOrderDelivered, true)} style={[styles.actionButton, !deliveryEnabled && styles.toggleDisabled]}>
              <Text style={styles.deliveredButtonText}>{updatingOrder ? 'Updating…' : 'Confirm delivery'}</Text>
            </Pressable>
          </View>
          {!pickupEnabled && !deliveryEnabled && <Text style={styles.hint}>{availability.pickupHint}</Text>}
        </View> : <Text style={styles.empty}>{onDuty ? 'No order is currently offered. You will receive the oldest ready order nearby.' : 'Go on duty to receive delivery offers.'}</Text>}
      </>}
      <Text style={styles.sectionHeading}>Delivered orders</Text>
      {history.length === 0 ? <Text style={styles.empty}>No completed deliveries yet.</Text> : history.map((entry) =>
        <View key={entry.orderId} style={styles.orderCard}>
          <Text style={styles.orderTitle}>Order #{entry.orderId}</Text>
          <Text style={styles.orderMuted}>{entry.status.replaceAll('_', ' ').toLowerCase()} · {entry.deliveredAt ? new Date(entry.deliveredAt).toLocaleString() : 'Delivery time unavailable'}</Text>
          <Text style={styles.orderLine}>Total charge {money(entry.totalCharge)}</Text>
        </View>)}
      {!!message && <Text accessibilityRole="alert" style={styles.message}>{message}</Text>}
      <View style={styles.map}><CityMap refreshKey={mapRefreshKey} onData={setMapData} /></View>
    </ScrollView>
  </SafeAreaView>;
}
