import { useEffect, useState } from 'react';
import { Redirect } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AccountMenu from '@/components/AccountMenu';
import CityMap from '@/components/dash/CityMap';
import type { MapData } from '@/types/dash';
import { queueBand, queueBandColor } from '@/lib/queueBands';
import { useAuth } from '@/context/AuthContext';
import { acceptOrder, declineOrder, fetchCourierDuty, goOffDuty, goOnDuty, markOrderDelivered, markOrderPickedUp, type CourierDuty } from '@/api/courierDuty';
import { canGoOffDuty, canGoOnDuty, getOrderActionAvailability, hasActiveDelivery, isOnDuty, stateLabel } from '@/lib/courierDuty';
import { styles } from './couriers.styles';

// Orders can be assigned at any time, so the panel refreshes itself while open
const REFRESH_MS = 15000;
const money = (value: number | null) => value == null ? '' : `$${Number(value).toFixed(2)}`;

export default function CourierDashboard() {
  const { user, token, ready } = useAuth();
  const [duty, setDuty] = useState<CourierDuty | null>(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [message, setMessage] = useState('');
  // Going on or off duty changes the courier's map presence, so refresh it immediately.
  const [mapRefreshKey, setMapRefreshKey] = useState(0);
  const [mapData, setMapData] = useState<MapData | null>(null);
  const order = duty?.assignedOrder;
  const pickupRestaurant = order && mapData?.restaurants.find((item) => item.name === order.restaurantName);

  useEffect(() => {
    if (!token || user?.role !== 'courier') return;
    let active = true;
    const refresh = async () => {
      try {
        const loaded = await fetchCourierDuty(token);
        if (active) setDuty(loaded);
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

  if (!ready) return <SafeAreaView style={styles.page}><ActivityIndicator color="#D42F50" /></SafeAreaView>;
  if (!user) return <Redirect href="/auth/login" />;
  if (user.role !== 'courier') return <Redirect href="/account" />;

  const onDuty = duty != null && isOnDuty(duty.status);
  const toggleEnabled = duty != null && !updating && (onDuty ? canGoOffDuty(duty.status) : canGoOnDuty(duty.status));
  const orderActions = getOrderActionAvailability(duty?.status, order?.status);
  const { pickupHint, deliveryHint } = orderActions;
  const showDeliveryActions = duty != null && order != null && hasActiveDelivery(duty.status);
  const pickupEnabled = !updating && orderActions.pickupEnabled;
  const deliveryEnabled = !updating && orderActions.deliveryEnabled;
  const actionHint = duty?.status === 'assigned'
    ? order?.status === 'ready' ? deliveryHint : pickupHint
    : duty?.status === 'carrying' ? deliveryHint : undefined;

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
          {pickupRestaurant && <View style={styles.queueRow}><View style={[styles.queueDot, { backgroundColor: queueBandColor[queueBand(pickupRestaurant.queueCount, pickupRestaurant.status)] }]} /><Text style={styles.orderMuted}>(Queue Length: {pickupRestaurant.queueCount})</Text></View>}
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
          </View>
          }
          {showDeliveryActions && <>
          <View style={styles.actionRow}>
            <Pressable
              accessibilityRole="button"
              accessibilityHint={duty.status === 'assigned' ? pickupHint : undefined}
              accessibilityState={{ disabled: !pickupEnabled }}
              disabled={!pickupEnabled}
              onPress={() => void performOrderAction(markOrderPickedUp, 'Could not confirm pickup.')}
              style={[styles.actionButton, !pickupEnabled && styles.toggleDisabled]}>
              <Text style={styles.deliveredButtonText}>{updating ? 'Updating…' : 'Confirm pickup'}</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityHint={deliveryHint}
              accessibilityState={{ disabled: !deliveryEnabled }}
              disabled={!deliveryEnabled}
              onPress={() => void performOrderAction(markOrderDelivered, 'Could not confirm delivery.')}
              style={[styles.actionButton, !deliveryEnabled && styles.toggleDisabled]}>
              <Text style={styles.deliveredButtonText}>{updating ? 'Updating…' : 'Confirm delivery'}</Text>
            </Pressable>
          </View>
          {!!actionHint && <Text style={styles.hint}>{actionHint}</Text>}
        </>}
          
        </View> : <Text style={styles.empty}>{onDuty ? 'No order is currently offered. You will receive the oldest ready order nearby.' : 'Go on duty to receive delivery offers.'}</Text>}
      </>}
      {!!message && <Text style={styles.message}>{message}</Text>}
      <View style={styles.map}><CityMap refreshKey={mapRefreshKey} onData={setMapData} /></View>
    </ScrollView>
  </SafeAreaView>;
}
