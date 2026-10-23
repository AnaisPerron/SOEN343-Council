import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Animated, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useCart } from '@/context/CartContext';
import { useOrders } from '@/context/OrdersContext';
import { menuItemsMock } from '@/mocks/menus.mock';
import { restaurantsMock } from '@/mocks/restaurants.mock';
import { groupCartLinesByRestaurant } from '../cart/cartStore';
import { getCartOrderRestriction, type RestaurantOrderRestriction } from '../services/restaurantOrderRules';
import { styles } from './confirm-order.styles';

type OrderPhase = 'review' | 'submitting' | 'success';

export default function ConfirmOrderScreen() {
  const router = useRouter();
  const { lines, clearCart } = useCart();
  const { orders, placeOrders, cancelOrder } = useOrders();
  const [phase, setPhase] = useState<OrderPhase>('review');
  const [placedIds, setPlacedIds] = useState<string[]>([]);
  const [restriction, setRestriction] = useState<RestaurantOrderRestriction | null>(null);
  const [successOpacity] = useState(() => new Animated.Value(0));
  const [successScale] = useState(() => new Animated.Value(0.88));
  const submitTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const sending = useRef(false);
  const groups = groupCartLinesByRestaurant(lines);
  const total = groups.reduce((sum, group) => sum + group.total, 0);
  const placedOrders = orders.filter((order) => placedIds.includes(order.id));

  useEffect(() => () => {
    if (submitTimer.current) clearTimeout(submitTimer.current);
    successOpacity.stopAnimation();
    successScale.stopAnimation();
  }, [successOpacity, successScale]);

  const goBack = () => phase === 'success'
    ? router.replace('/customer/home/home')
    : router.canGoBack() ? router.back() : router.replace('/customer/cart/cart');

  const sendOrders = () => {
    if (sending.current || phase !== 'review' || lines.length === 0) return;
    const blocked = getCartOrderRestriction(groups.map((group) => group.restaurantId), restaurantsMock);
    if (blocked) {
      setRestriction(blocked);
      return;
    }

    setRestriction(null);
    sending.current = true;
    setPhase('submitting');
    // TODO: Send each restaurant's order to the backend when its order endpoint is available.
    // This demo delay creates local order records without making a request.
    submitTimer.current = setTimeout(() => {
      const placed = placeOrders(groups);
      setPlacedIds(placed.map((order) => order.id));
      clearCart();
      setPhase('success');
      Animated.parallel([
        Animated.timing(successOpacity, { toValue: 1, duration: 280, useNativeDriver: true }),
        Animated.spring(successScale, { toValue: 1, friction: 6, tension: 75, useNativeDriver: true }),
      ]).start();
    }, 1100);
  };

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
          {placedOrders.map((order) => {
            const restaurant = restaurantsMock.find((entry) => entry.id === order.restaurantId);
            return <View key={order.id} style={styles.statusPanel}>
              <Text style={styles.statusRestaurant}>{restaurant?.name ?? 'Restaurant'}</Text>
              <Text style={styles.statusSubtitle}>Order #{order.id} · ${order.total.toFixed(2)}</Text>
              <View style={styles.statusStep}><View style={styles.statusDotGrey} /><Text style={styles.statusCurrent}>{order.status === 'cancelled' ? 'Placed' : 'Placed · awaiting restaurant'}</Text></View>
              {order.status === 'cancelled' ? (
                <View style={styles.statusStep}><View style={styles.statusDotCancelled} /><Text style={styles.statusCancelled}>Cancelled by you</Text></View>
              ) : <>
                <View style={styles.statusStep}><View style={styles.statusDotFuture} /><Text style={styles.statusFuture}>Accepted or rejected</Text></View>
                <View style={styles.statusStep}><View style={styles.statusDotPreparing} /><Text style={styles.statusFuture}>Preparing</Text></View>
                <View style={styles.statusStep}><View style={styles.statusDotReady} /><Text style={styles.statusFuture}>Ready</Text></View>
                <TouchableOpacity style={styles.cancelButton} accessibilityRole="button" accessibilityLabel={`Cancel order for ${restaurant?.name ?? 'Restaurant'}`} onPress={() => cancelOrder(order.id)}><Text style={styles.cancelButtonText}>Cancel order</Text></TouchableOpacity>
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
          <Text style={styles.sectionTitle}>Review your order</Text>
          {groups.map((group) => {
            const restaurant = restaurantsMock.find((entry) => entry.id === group.restaurantId);
            return <View key={group.restaurantId} style={styles.restaurantBlock}>
              <Text style={styles.restaurantName}>{restaurant?.name ?? 'Restaurant'}</Text>
              {group.lines.map((line) => {
                const item = menuItemsMock.find((entry) => entry.id === line.itemId);
                return <View key={line.key} style={styles.itemRow}>
                  <Text style={styles.itemName}>{line.quantity} × {item?.name ?? 'Menu item'}</Text>
                  <Text style={styles.itemPrice}>${(line.quantity * line.unitPrice).toFixed(2)}</Text>
                </View>;
              })}
            </View>;
          })}
          <View style={styles.totalRow}><Text style={styles.totalLabel}>Estimated total</Text><Text style={styles.totalValue}>${total.toFixed(2)}</Text></View>

          <View style={styles.paymentPanel}>
            <Text style={styles.paymentLabel}>PAYMENT</Text>
            <Text style={styles.paymentTitle}>Payment info upcoming</Text>
            <Text style={styles.paymentText}>Payment details will be added when checkout is connected.</Text>
          </View>
          {restriction && <View style={styles.restrictionPanel} accessibilityRole="alert">
            <Text style={styles.restrictionCode}>{restriction.code}</Text>
            <Text style={styles.restrictionText}>{restriction.message}</Text>
          </View>}
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
