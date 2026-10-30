import { useEffect, useRef, useState } from 'react';
import { Animated, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useCart } from '@/context/CartContext';
import { groupCartLinesByRestaurant } from './cartStore';
import { menuItemOptionsMock, menuItemsMock } from '@/mocks/menus.mock';
import { restaurantsMock } from '@/mocks/restaurants.mock';
import { restaurantDisplayMock } from '@/mocks/restaurantDisplay.mock';
import { getCartOrderRestriction, ORDER_ERROR_FADE_DELAY_MS, ORDER_ERROR_FADE_DURATION_MS, type RestaurantOrderRestriction } from '../services/restaurantOrderRules';
import { styles } from './cart.styles';

export default function CartScreen() {
  const router = useRouter();
  const { lines, setQuantity } = useCart();
  const [orderError, setOrderError] = useState<RestaurantOrderRestriction | null>(null);
  const [toastOpacity] = useState(() => new Animated.Value(0));
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  // The cart badge counts units, while each rendered row represents one item/extras combination.
  const count = lines.reduce((total, line) => total + line.quantity, 0);
  const groups = groupCartLinesByRestaurant(lines);
  const grandTotal = groups.reduce((total, group) => total + group.total, 0);
  const goBack = () => router.canGoBack() ? router.back() : router.replace('/customer/home/home');

  useEffect(() => () => {
    if (toastTimer.current) clearTimeout(toastTimer.current);
    toastOpacity.stopAnimation();
  }, [toastOpacity]);

  const makeOrder = () => {
    const restriction = getCartOrderRestriction(groups.map((group) => group.restaurantId), restaurantsMock);
    if (restriction) {
      setOrderError(restriction);
      if (toastTimer.current) clearTimeout(toastTimer.current);
      toastOpacity.stopAnimation();
      toastOpacity.setValue(1);
      toastTimer.current = setTimeout(() => {
        Animated.timing(toastOpacity, {
          toValue: 0,
          duration: ORDER_ERROR_FADE_DURATION_MS,
          useNativeDriver: true,
        }).start(({ finished }) => {
          if (finished) setOrderError(null);
        });
      }, ORDER_ERROR_FADE_DELAY_MS);
      return;
    }
    router.push('/customer/checkout/confirm-order');
  };

  return (
    <SafeAreaView style={styles.page} edges={['top', 'bottom']}>
      <View style={styles.header}>
        <TouchableOpacity accessibilityRole="button" accessibilityLabel="Back" onPress={goBack} style={styles.backButton}><Text style={styles.backText}>‹</Text></TouchableOpacity>
        <Text style={styles.title}>Cart</Text>
        <View style={styles.headerSpacer} />
      </View>
      <ScrollView style={styles.scroll} contentContainerStyle={styles.content}>
        {lines.length === 0 ? <View style={styles.empty}>
          <Text style={styles.emptyTitle}>Your cart is empty</Text>
          <Text style={styles.emptyText}>Choose a menu item to start your order.</Text>
          <TouchableOpacity accessibilityRole="button" onPress={() => router.replace('/customer/home/home')} style={styles.browseButton}><Text style={styles.browseText}>Browse restaurants</Text></TouchableOpacity>
        </View> : <>
          <Text style={styles.count}>{count} {count === 1 ? 'item' : 'items'} in your cart</Text>
          {groups.map((group) => {
            // Each restaurant gets a separate vertical bill and subtotal.
            const restaurant = restaurantsMock.find((entry) => entry.id === group.restaurantId);
            const address = restaurantDisplayMock[group.restaurantId]?.address;
            const status = restaurant?.status === 'open' ? 'Open now' : restaurant?.status === 'paused' ? 'Temporarily paused' : 'Closed now';
            return <View key={group.restaurantId} style={styles.restaurantSection}>
              <Text style={styles.restaurantName}>{restaurant?.name ?? 'Restaurant'}</Text>
              <View style={styles.restaurantDetails}>
                <Text style={styles.restaurantDetailText}>{address ?? 'Address unavailable'}</Text>
                <Text style={styles.restaurantStatus}>{status}</Text>
              </View>
              <View style={styles.divider} />
              {group.lines.map((line) => {
                // The cart stores IDs and prices; resolve fixture names only for display.
                const item = menuItemsMock.find((entry) => entry.id === line.itemId);
                const selectedOptions = menuItemOptionsMock.filter((option) => line.optionIds.includes(option.id));
                if (!item) return null;
                return <View key={line.key} style={styles.line}>
                  <View style={styles.lineTop}>
                    <Text style={styles.itemName}>{item.name}</Text>
                    <Text style={styles.linePrice}>${(line.unitPrice * line.quantity).toFixed(2)}</Text>
                  </View>
                  {selectedOptions.length > 0 && <Text style={styles.options}>{selectedOptions.map((option) => option.name).join(', ')}</Text>}
                  <Text style={styles.unitPrice}>{line.quantity} × ${line.unitPrice.toFixed(2)} each</Text>
                  {/* The store removes this row when its quantity drops from one to zero. */}
                  <View style={styles.quantityControl}>
                    <TouchableOpacity accessibilityRole="button" accessibilityLabel={`Remove one ${item.name}`} onPress={() => setQuantity(line.key, line.quantity - 1)} style={styles.quantityButton}><Text style={styles.quantityButtonText}>−</Text></TouchableOpacity>
                    <Text style={styles.quantityText}>{line.quantity}</Text>
                    <TouchableOpacity accessibilityRole="button" accessibilityLabel={`Add one ${item.name}`} onPress={() => setQuantity(line.key, line.quantity + 1)} style={styles.quantityButton}><Text style={styles.quantityButtonText}>+</Text></TouchableOpacity>
                  </View>
                </View>;
              })}
              {/* Fees are shown as $0.00 until pricing is supported. */}
              <View style={styles.billRows}>
                <View style={styles.billRow}><Text style={styles.billLabel}>Subtotal</Text><Text style={styles.billValue}>${group.subtotal.toFixed(2)}</Text></View>
                <View style={styles.billRow}><Text style={styles.billLabel}>Delivery fee</Text><Text style={styles.billValue}>${group.deliveryFee.toFixed(2)}</Text></View>
                <View style={styles.billRow}><Text style={styles.billLabel}>Service fee</Text><Text style={styles.billValue}>${group.serviceFee.toFixed(2)}</Text></View>
              </View>
              <View style={styles.restaurantTotal}><Text style={styles.restaurantTotalLabel}>Restaurant total</Text><Text style={styles.restaurantTotalValue}>${group.total.toFixed(2)}</Text></View>
            </View>;
          })}
          {/* Sum the restaurant bills for the cart's displayed total. */}
          <View style={styles.totalRow}><Text style={styles.totalLabel}>Estimated total</Text><Text style={styles.totalValue}>${grandTotal.toFixed(2)}</Text></View>
        </>}
      </ScrollView>
      <View style={styles.footer}>
        <TouchableOpacity style={[styles.makeOrderButton, lines.length === 0 && styles.makeOrderButtonDisabled]} accessibilityRole="button" accessibilityState={{ disabled: lines.length === 0 }} disabled={lines.length === 0} onPress={makeOrder}>
          <Text style={styles.makeOrderText}>Make my order</Text>
        </TouchableOpacity>
      </View>
      {orderError && <View style={styles.orderErrorOverlay} pointerEvents="none">
        <Animated.View style={[styles.orderErrorToast, { opacity: toastOpacity }]} accessibilityRole="alert" accessibilityLiveRegion="assertive">
          <Text style={styles.orderErrorCode}>{orderError.code}</Text>
          <Text style={styles.orderErrorMessage}>{orderError.message}</Text>
        </Animated.View>
      </View>}
    </SafeAreaView>
  );
}
