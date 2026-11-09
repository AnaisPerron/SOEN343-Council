import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Animated, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useCart } from '@/context/CartContext';
import { useOrders, type OrderFailure } from '@/context/OrdersContext';
import { useCatalog } from '@/context/CatalogContext';
import { groupCartLinesByRestaurant } from '../cart/cartStore';
import { getCartOrderRestriction, type RestaurantOrderRestriction } from '../services/restaurantOrderRules';
import { styles } from './confirm-order.styles';

type OrderPhase = 'review' | 'submitting' | 'success';

export default function ConfirmOrderScreen() {
  const router = useRouter();
  const { lines, removeRestaurant } = useCart();
  const { restaurants, findItem } = useCatalog();
  const { orders, placeOrders, cancelOrder } = useOrders();
  const [phase, setPhase] = useState<OrderPhase>('review');
  const [placedIds, setPlacedIds] = useState<string[]>([]);
  const [restriction, setRestriction] = useState<RestaurantOrderRestriction | null>(null);
  const [failures, setFailures] = useState<OrderFailure[]>([]);
  const [cancelError, setCancelError] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<string | null>(null);
  const [successOpacity] = useState(() => new Animated.Value(0));
  const [successScale] = useState(() => new Animated.Value(0.88));
  const sending = useRef(false);
  const groups = groupCartLinesByRestaurant(lines);
  const total = groups.reduce((sum, group) => sum + group.total, 0);
  const placedOrders = orders.filter((order) => placedIds.includes(order.id));

  useEffect(() => () => {
    successOpacity.stopAnimation();
    successScale.stopAnimation();
  }, [successOpacity, successScale]);

  const goBack = () => phase === 'success'
    ? router.replace('/customer/home/home')
    : router.canGoBack() ? router.back() : router.replace('/customer/cart/cart');

  const sendOrders = async () => {
    if (sending.current || phase !== 'review' || lines.length === 0) return;
    const blocked = getCartOrderRestriction(groups.map((group) => group.restaurantId), restaurants);
    if (blocked) {
      setRestriction(blocked);
      return;
    }

    setRestriction(null);
    setFailures([]);
    sending.current = true;
    setPhase('submitting');
    const result = await placeOrders(groups);
    sending.current = false;
    // Only placed restaurants leave the cart, so failed ones can be retried.
    result.placed.forEach((order) => removeRestaurant(order.restaurantId));
    setFailures(result.failures);
    if (result.placed.length === 0) {
      setPhase('review');
      return;
    }
    setPlacedIds(result.placed.map((order) => order.id));
    setPhase('success');
    Animated.parallel([
      Animated.timing(successOpacity, { toValue: 1, duration: 280, useNativeDriver: true }),
      Animated.spring(successScale, { toValue: 1, friction: 6, tension: 75, useNativeDriver: true }),
    ]).start();
  };

  const cancel = (orderId: string) => {
    if (cancellingId) return;
    setCancelError(null);
    setCancellingId(orderId);
    cancelOrder(orderId)
      .catch((error) => setCancelError(error instanceof Error ? error.message : 'Could not cancel the order.'))
      .finally(() => setCancellingId(null));
  };

  const restaurantName = (restaurantId: number) => restaurants.find((entry) => entry.id === restaurantId)?.name ?? 'Restaurant';
  const failurePanel = failures.length > 0 && <View style={styles.restrictionPanel} accessibilityRole="alert">
    <Text style={styles.restrictionCode}>{phase === 'success' ? 'Some orders were not placed' : 'Order not placed'}</Text>
    {failures.map((failure) => <Text key={failure.restaurantId} style={styles.restrictionText}>{restaurantName(failure.restaurantId)}: {failure.message}</Text>)}
    {phase === 'success' && <Text style={styles.restrictionText}>Those items are still in your cart.</Text>}
  </View>;

  return (
    <SafeAreaView style={styles.page} edges={['top', 'bottom']}>
      <View style={styles.header}>
        <TouchableOpacity accessibilityRole="button" accessibilityLabel={phase === 'success' ? 'Back to restaurants' : 'Back to cart'} disabled={phase === 'submitting'} onPress={goBack} style={styles.backButton}>
          <Text style={styles.backText}>{'\u2039'}</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Confirm order</Text>
        <View style={styles.headerSpacer} />
      </View>

      {phase === 'success' ? (
        <ScrollView style={styles.scroll} contentContainerStyle={styles.successContent}>
          {placedOrders.some((order) => order.status === 'placed') && <Text style={styles.successMessage}>You can still cancel the order before it is accepted or rejected by the restaurant. See Profile → History to check the status of your order.</Text>}
          <Animated.View style={[styles.successPanel, { opacity: successOpacity, transform: [{ scale: successScale }] }]} accessibilityRole="alert" accessibilityLiveRegion="polite">
            <View style={styles.successIcon}><Text style={styles.successCheck}>{'\u2713'}</Text></View>
            <Text style={styles.successTitle}>{placedOrders.every((order) => order.status === 'cancelled') ? 'Your order has been cancelled.' : 'Your order has been placed.'}</Text>
          </Animated.View>
          {failurePanel}
          {cancelError && <View style={styles.restrictionPanel} accessibilityRole="alert"><Text style={styles.restrictionText}>{cancelError}</Text></View>}
          {placedOrders.map((order) => {
            const restaurant = restaurants.find((entry) => entry.id === order.restaurantId);
            const summaryItems = order.items?.map((item) => ({ key: `${item.name}-${item.unitPrice}`, name: item.name, quantity: item.quantity, unitPrice: item.unitPrice }))
              ?? order.lines.map((line) => ({ key: line.key, name: findItem(line.itemId)?.name ?? 'Menu item', quantity: line.quantity, unitPrice: line.unitPrice }));
            const itemsSubtotal = summaryItems.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0);
            const otherCharges = order.total - itemsSubtotal;
            return <View key={order.id} style={styles.statusPanel}>
              <Text style={styles.statusRestaurant}>{restaurant?.name ?? 'Restaurant'}</Text>
              <Text style={styles.statusSubtitle}>Order #{order.id}</Text>
              <View style={styles.placedSummary}>
                <Text style={styles.placedSummaryTitle}>Order summary</Text>
                <View style={styles.itemRow}><Text style={styles.itemName}>Placed at</Text><Text style={styles.itemPrice}>{order.createdAt ? new Date(order.createdAt).toLocaleString() : 'Unavailable'}</Text></View>
                {summaryItems.map((item, index) => <View key={`${item.key}-${index}`} style={styles.itemRow}>
                  <Text style={styles.itemName}>{item.quantity} × {item.name}</Text>
                  <Text style={styles.itemPrice}>${(item.quantity * item.unitPrice).toFixed(2)}</Text>
                </View>)}
                <View style={styles.itemRow}><Text style={styles.itemName}>Items subtotal</Text><Text style={styles.itemPrice}>${itemsSubtotal.toFixed(2)}</Text></View>
                {/* It shows the “Additional charges” row only when the order total differs from the sum of its items.*/}
                {Math.abs(otherCharges) > 0.005 && <View style={styles.itemRow}><Text style={styles.itemName}>Additional charges</Text><Text style={styles.itemPrice}>${otherCharges.toFixed(2)}</Text></View>}
                <View style={styles.itemRow}><Text style={styles.totalLabel}>Total</Text><Text style={styles.totalValue}>${order.total.toFixed(2)}</Text></View>
              </View>
              <View style={styles.statusStep}><View style={styles.statusDotGrey} /><Text style={styles.statusCurrent}>{order.status === 'cancelled' ? 'Placed' : 'Placed · awaiting restaurant'}</Text></View>
              {order.status === 'cancelled' ? (
                <View style={styles.statusStep}><View style={styles.statusDotCancelled} /><Text style={styles.statusCancelled}>Cancelled by you</Text></View>
              ) : <>
                <View style={styles.statusStep}><View style={styles.statusDotFuture} /><Text style={styles.statusFuture}>Accepted or rejected</Text></View>
                <View style={styles.statusStep}><View style={styles.statusDotPreparing} /><Text style={styles.statusFuture}>Preparing</Text></View>
                <View style={styles.statusStep}><View style={styles.statusDotReady} /><Text style={styles.statusFuture}>Ready</Text></View>
                <TouchableOpacity style={styles.cancelButton} accessibilityRole="button" accessibilityLabel={`Cancel order for ${restaurant?.name ?? 'Restaurant'}`} disabled={cancellingId !== null} onPress={() => cancel(order.id)}><Text style={styles.cancelButtonText}>{cancellingId === order.id ? 'Cancelling...' : 'Cancel order'}</Text></TouchableOpacity>
              </>}
            </View>;
          })}
        </ScrollView>
      ) : lines.length === 0 ? (
        <View style={styles.center}>
          <Text style={styles.emptyTitle}>Your cart is empty</Text>
          <Text style={styles.emptyText}>Add an item before confirming an order.</Text>
        </View>
      ) : (
        <ScrollView style={styles.scroll} contentContainerStyle={styles.content}>
          <Text style={styles.sectionTitle}>Order summary</Text>
          {groups.map((group) => {
            const restaurant = restaurants.find((entry) => entry.id === group.restaurantId);
            return <View key={group.restaurantId} style={styles.restaurantBlock}>
              <Text style={styles.restaurantName}>{restaurant?.name ?? 'Restaurant'}</Text>
              {group.lines.map((line) => {
                const item = findItem(line.itemId);
                return <View key={line.key} style={styles.itemRow}>
                  <Text style={styles.itemName}>{line.quantity} × {item?.name ?? 'Menu item'}</Text>
                  <Text style={styles.itemPrice}>${(line.quantity * line.unitPrice).toFixed(2)}</Text>
                </View>;
              })}
            </View>;
          })}
          <View style={styles.totalRow}><Text style={styles.totalLabel}>Total</Text><Text style={styles.totalValue}>${total.toFixed(2)}</Text></View>

          <View style={styles.paymentPanel}>
            <Text style={styles.paymentLabel}>PAYMENT</Text>
            <Text style={styles.paymentTitle}>Payment info upcoming</Text>
            <Text style={styles.paymentText}>Payment details will be added when checkout is connected.</Text>
          </View>
          {restriction && <View style={styles.restrictionPanel} accessibilityRole="alert">
            <Text style={styles.restrictionCode}>{restriction.code}</Text>
            <Text style={styles.restrictionText}>{restriction.message}</Text>
          </View>}
          {failurePanel}
        </ScrollView>
      )}

      <View style={styles.footer}>
        {phase === 'success' ? (
          <TouchableOpacity style={styles.sendButton} accessibilityRole="button" onPress={() => router.replace('/customer/home/home')}><Text style={styles.sendButtonText}>Browse restaurants</Text></TouchableOpacity>
        ) : (
          <TouchableOpacity style={[styles.sendButton, (phase === 'submitting' || lines.length === 0) && styles.sendButtonDisabled]} accessibilityRole="button" accessibilityLabel="Send orders" accessibilityState={{ disabled: phase === 'submitting' || lines.length === 0, busy: phase === 'submitting' }} disabled={phase === 'submitting' || lines.length === 0} onPress={sendOrders}>
            {phase === 'submitting' ? <><ActivityIndicator color="#FFFFFF" size="small" /><Text style={styles.sendButtonText}>Placing orders...</Text></> : <Text style={styles.sendButtonText}>Send orders</Text>}
          </TouchableOpacity>
        )}
      </View>
    </SafeAreaView>
  );
}
