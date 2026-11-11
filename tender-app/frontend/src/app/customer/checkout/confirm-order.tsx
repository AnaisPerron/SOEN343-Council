import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, Animated, Modal, ScrollView, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useCart } from '@/context/CartContext';
import { useOrders, type OrderFailure } from '@/context/OrdersContext';
import { useCatalog } from '@/context/CatalogContext';
import { useAuth } from '@/context/AuthContext';
import { fetchPricingRules, type PricingRules } from '@/api/pricing';
import { fetchTenderCash } from '@/api/tenderCash';
import type { OrderSubmissionResult } from '../services/orderApi';
import { API_URL } from '@/api/client';
import AddressAutocomplete, { type AddressLookup, type PickedAddress } from '@/components/AddressAutocomplete';
import DeliveryMap from './DeliveryMap';
import PayPalButton from './PayPalButton';
import { groupCartLinesByRestaurant } from '../cart/cartStore';
import { getCartOrderRestriction, type RestaurantOrderRestriction } from '../services/restaurantOrderRules';
import { fetchOrderQuote, toOrderSubmission, type OrderQuote } from '../services/orderApi';
import { styles } from './confirm-order.styles';

type OrderPhase = 'review' | 'submitting' | 'success';
const splitAddress = (address: string) => {
  const parts = address.split(',').map((part) => part.trim());
  return { line: parts.slice(0, Math.max(1, parts.length - 2)).join(', '),
    city: parts.length >= 2 ? parts[parts.length - 2] : '', country: parts.length >= 3 ? parts[parts.length - 1] : '' };
};

export default function ConfirmOrderScreen() {
  const router = useRouter();
  const { lines, removeRestaurant } = useCart();
  const { restaurants, findItem } = useCatalog();
  const { orders, placeOrders, recordPaidOrders, cancelOrder } = useOrders();
  const { user, token } = useAuth();
  const [pricingRules, setPricingRules] = useState<PricingRules | null>(null);
  const [pricingError, setPricingError] = useState('');
  const [pricingLoading, setPricingLoading] = useState(true);
  const [pricingRetry, setPricingRetry] = useState(0);
  const [showPricingInfo, setShowPricingInfo] = useState(false);
  const [quotes, setQuotes] = useState<Record<number, OrderQuote>>({});
  const [quoteLoading, setQuoteLoading] = useState(true);
  const [quoteError, setQuoteError] = useState('');
  const [quoteRetry, setQuoteRetry] = useState(0);
  const savedAddress = user?.address ?? '';
  const [addressLine, setAddressLine] = useState(() => splitAddress(savedAddress).line);
  const [city, setCity] = useState(() => splitAddress(savedAddress).city);
  const [country, setCountry] = useState(() => splitAddress(savedAddress).country);
  const [destination, setDestination] = useState<{ latitude: number; longitude: number } | null>(null);
  const [addressLookup, setAddressLookup] = useState<AddressLookup | null>(null);
  const onLookup = useCallback((value: AddressLookup) => setAddressLookup(value), []);
  const [addressError, setAddressError] = useState('');
  const [phase, setPhase] = useState<OrderPhase>('review');
  const [placedIds, setPlacedIds] = useState<string[]>([]);
  const [restriction, setRestriction] = useState<RestaurantOrderRestriction | null>(null);
  const [failures, setFailures] = useState<OrderFailure[]>([]);
  const [paypalError, setPaypalError] = useState('');
  const [tenderBalance, setTenderBalance] = useState(0);
  const [useTenders, setUseTenders] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<string | null>(null);
  const [successOpacity] = useState(() => new Animated.Value(0));
  const [successScale] = useState(() => new Animated.Value(0.88));
  const sending = useRef(false);
  const groups = useMemo(() => groupCartLinesByRestaurant(lines), [lines]);
  const quotedTotal = groups.reduce((sum, group) => sum + (quotes[group.restaurantId]?.total ?? 0), 0);
  const tendersApplied = useTenders ? Math.min(tenderBalance, Math.floor((quotedTotal + 0.00001) * 10)) : 0;
  const tenderAllocations = groups.reduce((result, group) => {
    const used = Object.values(result).reduce((sum, value) => sum + value, 0);
    result[group.restaurantId] = Math.min(Math.max(0, tendersApplied - used), Math.floor(((quotes[group.restaurantId]?.total ?? 0) + 0.00001) * 10));
    return result;
  }, {} as Record<number, number>);
  const amountDue = Math.round((quotedTotal - tendersApplied / 10) * 100) / 100;
  const quotesReady = groups.length > 0 && groups.every((group) => quotes[group.restaurantId] != null);
  const placedOrders = orders.filter((order) => placedIds.includes(order.id));
  const deliveryAddress = [addressLine.trim(), city.trim(), country.trim()].join(', ');
  const restaurantPoints = groups.flatMap((group) => {
    const restaurant = restaurants.find((entry) => entry.id === group.restaurantId);
    return restaurant?.latitude != null && restaurant.longitude != null
      ? [{ id: restaurant.id, name: restaurant.name, status: restaurant.status, latitude: restaurant.latitude, longitude: restaurant.longitude }] : [];
  });

  useEffect(() => () => {
    successOpacity.stopAnimation();
    successScale.stopAnimation();
  }, [successOpacity, successScale]);

  useEffect(() => {
    if (!token) return;
    let active = true;
    fetchTenderCash(token).then((balance) => { if (active) setTenderBalance(balance.tenders); })
      .catch(() => { if (active) setTenderBalance(0); });
    return () => { active = false; };
  }, [token]);

  useEffect(() => {
    if (!token) return;
    let active = true;
    setPricingLoading(true);
    setPricingError('');
    fetchPricingRules(token)
      .then((rules) => { if (active) setPricingRules(rules); })
      .catch((error) => { if (active) setPricingError(error instanceof Error ? error.message : 'Could not load pricing rules.'); })
      .finally(() => { if (active) setPricingLoading(false); });
    return () => { active = false; };
  }, [token, pricingRetry]);

  useEffect(() => {
    if (!token || groups.length === 0) { setQuoteLoading(false); return; }
    const changed = deliveryAddress !== savedAddress.trim();
    if (!addressLine.trim() || !city.trim() || !country.trim() || (changed && !destination)) {
      setQuotes({}); setQuoteLoading(false); setQuoteError(''); return;
    }
    let active = true;
    setQuotes({}); setQuoteLoading(true); setQuoteError('');
    const override = destination ? { address: deliveryAddress, ...destination } : undefined;
    Promise.all(groups.map(async (group) => [group.restaurantId,
      await fetchOrderQuote(`${API_URL}/api/orders/quote`, token, toOrderSubmission(group, override))] as const))
      .then((entries) => { if (active) setQuotes(Object.fromEntries(entries)); })
      .catch((error) => { if (active) setQuoteError(error instanceof Error ? error.message : 'Could not calculate the order total.'); })
      .finally(() => { if (active) setQuoteLoading(false); });
    return () => { active = false; };
  }, [token, groups, deliveryAddress, savedAddress, addressLine, city, country, destination, quoteRetry]);

  const goBack = () => phase === 'success'
    ? router.replace('/customer/home/home')
    : router.canGoBack() ? router.back() : router.replace('/customer/cart/cart');

  const validateCheckout = () => {
    if (sending.current || phase !== 'review' || lines.length === 0 || quoteLoading || !quotesReady || quoteError) return false;
    const blocked = getCartOrderRestriction(groups.map((group) => group.restaurantId), restaurants);
    if (blocked) {
      setRestriction(blocked);
      return false;
    }
    if (!addressLine.trim() || !city.trim() || !country.trim()) {
      setAddressError('Address line, city, and country are required.'); return false;
    }
    if (deliveryAddress.length > 255) { setAddressError('Address must be at most 255 characters.'); return false; }
    const changed = deliveryAddress !== savedAddress.trim();
    if (changed && !destination) {
      setAddressError(addressLookup === 'unavailable'
        ? 'Address verification is unavailable. Retry the search before ordering.'
        : 'Pick a verified street address from the suggestions before ordering.');
      return false;
    }
    setRestriction(null);
    setAddressError('');
    setFailures([]);
    setPaypalError('');
    return true;
  };

  const completeOrders = (placed: { id: string; restaurantId: number }[], orderFailures: OrderFailure[]) => {
    sending.current = false;
    placed.forEach((order) => removeRestaurant(order.restaurantId));
    setFailures(orderFailures);
    if (placed.length === 0) { setPhase('review'); return; }
    setPlacedIds(placed.map((order) => order.id));
    setPhase('success');
    Animated.parallel([
      Animated.timing(successOpacity, { toValue: 1, duration: 280, useNativeDriver: true }),
      Animated.spring(successScale, { toValue: 1, friction: 6, tension: 75, useNativeDriver: true }),
    ]).start();
  };

  const sendOrders = async () => {
    if (!validateCheckout()) return;
    sending.current = true;
    setPhase('submitting');
    const result = await placeOrders(groups, destination
      ? { address: deliveryAddress, ...destination } : undefined, tenderAllocations);
    completeOrders(result.placed, result.failures);
  };

  const beginPayPal = () => {
    if (!validateCheckout()) return false;
    sending.current = true;
    setPhase('submitting');
    return true;
  };
  const failPayPal = (message: string) => {
    sending.current = false;
    setPhase('review');
    setPaypalError(message);
  };
  const paidWithPayPal = (receipts: OrderSubmissionResult[]) => {
    try { completeOrders(recordPaidOrders(groups, receipts), []); }
    catch (error) { failPayPal(error instanceof Error ? error.message : 'Payment completed, but the order could not be displayed. Check order history.'); }
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
          {paypalError ? <View style={styles.restrictionPanel} accessibilityRole="alert"><Text style={styles.restrictionText}>{paypalError}</Text></View> : null}
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
                {(tenderAllocations[order.restaurantId] ?? 0) > 0 && <View style={styles.itemRow}><Text style={styles.itemName}>Tender Cash used</Text><Text style={styles.itemPrice}>−${((tenderAllocations[order.restaurantId] ?? 0) / 10).toFixed(2)}</Text></View>}
                <View style={styles.itemRow}><Text style={styles.totalLabel}>Amount paid</Text><Text style={styles.totalValue}>${(order.total - (tenderAllocations[order.restaurantId] ?? 0) / 10).toFixed(2)}</Text></View>
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
            const quote = quotes[group.restaurantId];
            return <View key={group.restaurantId} style={styles.restaurantBlock}>
              <Text style={styles.restaurantName}>{restaurant?.name ?? 'Restaurant'}</Text>
              {group.lines.map((line) => {
                const item = findItem(line.itemId);
                return <View key={line.key} style={styles.itemRow}>
                  <Text style={styles.itemName}>{line.quantity} × {item?.name ?? 'Menu item'}</Text>
                  <Text style={styles.itemPrice}>${(line.quantity * line.unitPrice).toFixed(2)}</Text>
                </View>;
              })}
              {quote && <>
                <View style={styles.itemRow}><Text style={styles.itemName}>Items subtotal</Text><Text style={styles.itemPrice}>${quote.subtotal.toFixed(2)}</Text></View>
                <View style={styles.itemRow}><Text style={styles.itemName}>Service fee</Text><Text style={styles.itemPrice}>${quote.serviceFee.toFixed(2)}</Text></View>
                <View style={styles.itemRow}><Text style={styles.itemName}>Delivery ({quote.distanceKm.toFixed(2)} km)</Text><Text style={styles.itemPrice}>${quote.deliveryFee.toFixed(2)}</Text></View>
                <View style={styles.itemRow}><Text style={styles.itemName}>Small-order surcharge</Text><Text style={styles.itemPrice}>${quote.smallOrderFee.toFixed(2)}</Text></View>
                <View style={styles.itemRow}><Text style={styles.totalLabel}>Restaurant total</Text><Text style={styles.totalValue}>${quote.total.toFixed(2)}</Text></View>
              </>}
            </View>;
          })}
          {quoteLoading && <ActivityIndicator color="#D42F50" accessibilityLabel="Calculating order total" />}
          {quoteError ? <View style={styles.restrictionPanel} accessibilityRole="alert">
            <Text style={styles.restrictionText}>{quoteError}</Text>
            <TouchableOpacity accessibilityRole="button" onPress={() => setQuoteRetry((value) => value + 1)}>
              <Text style={styles.pricingRetry}>Retry total</Text>
            </TouchableOpacity>
          </View> : null}
          {quotesReady && !quoteLoading && <View style={styles.totalRow}>
            <View style={styles.totalLabelRow}>
              <Text style={styles.totalLabel}>Total to charge</Text>
              <TouchableOpacity style={styles.pricingInfoButton} accessibilityRole="button" accessibilityLabel="Show pricing rules"
                onPress={() => setShowPricingInfo(true)}>
                <Text style={styles.pricingInfoSymbol}>i</Text>
              </TouchableOpacity>
            </View>
            <Text style={styles.totalValue}>${quotedTotal.toFixed(2)}</Text>
          </View>}

          <View style={styles.addressPanel}>
            <Text style={styles.paymentTitle}>Delivery address</Text>
            <Text style={styles.paymentText}>This address applies to the orders placed now. Your profile address stays saved for future orders.</Text>
            <Text style={styles.addressLabel}>Country</Text>
            <TextInput style={styles.addressInput} value={country} onChangeText={(value) => { setCountry(value); setDestination(null); }} accessibilityLabel="Delivery country" />
            <Text style={styles.addressLabel}>City</Text>
            <TextInput style={styles.addressInput} value={city} onChangeText={(value) => { setCity(value); setDestination(null); }} accessibilityLabel="Delivery city" />
            <Text style={styles.addressLabel}>Address line</Text>
            <AddressAutocomplete style={styles.addressInput} value={addressLine}
              onChangeText={(value) => { setAddressLine(value); setDestination(null); }}
              onPick={(picked: PickedAddress) => {
                setAddressLine(picked.addressLine); setCity(picked.city); setCountry(picked.country);
                setDestination({ latitude: picked.latitude, longitude: picked.longitude }); setAddressError('');
              }} onLookup={onLookup} accessibilityLabel="Delivery address line" autoComplete="street-address" />
            {addressError ? <Text style={styles.addressError} accessibilityRole="alert">{addressError}</Text> : null}
            {!addressError && deliveryAddress !== savedAddress.trim() && !destination
              ? <Text style={styles.pricingNote}>Pick a verified street address to calculate the total.</Text> : null}
            <Text style={styles.mapTitle}>Delivery map</Text>
            <DeliveryMap address={deliveryAddress === savedAddress.trim() ? deliveryAddress : ''}
              destination={destination} restaurants={restaurantPoints} />
            {restaurantPoints.length < groups.length && <Text style={styles.paymentText}>Some restaurants do not have a saved map location.</Text>}
          </View>
          <View style={styles.paymentPanel}>
            <Text style={styles.paymentTitle}>Payment</Text>
            <Text style={styles.paymentText}>Tender Cash balance: {tenderBalance} tenders (${(tenderBalance / 10).toFixed(2)}). Earn 10 tenders per $1 paid.</Text>
            {tenderBalance > 0 && <TouchableOpacity accessibilityRole="checkbox" accessibilityState={{ checked: useTenders }} onPress={() => setUseTenders((value) => !value)} style={{ paddingVertical: 12 }}>
              <Text style={styles.paymentText}>{useTenders ? '☑' : '□'} Use Tender Cash ({tendersApplied} tenders, −${(tendersApplied / 10).toFixed(2)})</Text>
            </TouchableOpacity>}
            <Text style={styles.paymentText}>Amount due: ${amountDue.toFixed(2)}</Text>
            {token && amountDue > 0 && <PayPalButton token={token}
              orders={groups.map((group) => ({ ...toOrderSubmission(group, destination ? { address: deliveryAddress, ...destination } : undefined), tendersToUse: tenderAllocations[group.restaurantId] ?? 0 }))}
              disabled={phase === 'submitting' || lines.length === 0 || quoteLoading || !quotesReady || !!quoteError}
              onBegin={beginPayPal} onSuccess={paidWithPayPal} onFailure={failPayPal}
              onCancel={() => { sending.current = false; setPhase('review'); }} />}
            {paypalError ? <Text style={styles.addressError} accessibilityRole="alert">{paypalError}</Text> : null}
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
        ) : amountDue === 0 ? (
          <TouchableOpacity style={[styles.sendButton, (phase === 'submitting' || lines.length === 0 || quoteLoading || !quotesReady || !!quoteError) && styles.sendButtonDisabled]} accessibilityRole="button" accessibilityLabel={amountDue === 0 ? 'Pay with Tender Cash' : 'Send orders'} accessibilityState={{ disabled: phase === 'submitting' || lines.length === 0 || quoteLoading || !quotesReady || !!quoteError, busy: phase === 'submitting' }} disabled={phase === 'submitting' || lines.length === 0 || quoteLoading || !quotesReady || !!quoteError} onPress={sendOrders}>
            {phase === 'submitting' ? <><ActivityIndicator color="#FFFFFF" size="small" /><Text style={styles.sendButtonText}>Placing orders...</Text></> : <Text style={styles.sendButtonText}>{amountDue === 0 ? 'Pay with Tender Cash' : 'Send orders'}</Text>}
          </TouchableOpacity>
        ) : <View />}
      </View>
      <Modal transparent visible={showPricingInfo} animationType="fade" onRequestClose={() => setShowPricingInfo(false)}>
        <View style={styles.pricingBackdrop}>
          <TouchableOpacity style={styles.pricingBackdropDismiss} accessibilityRole="button" accessibilityLabel="Close pricing rules" onPress={() => setShowPricingInfo(false)} />
          <View style={styles.pricingDialog}>
            <View style={styles.pricingDialogHeader}>
              <Text style={styles.paymentTitle}>Pricing rules</Text>
              <TouchableOpacity accessibilityRole="button" accessibilityLabel="Close pricing rules" onPress={() => setShowPricingInfo(false)}>
                <Text style={styles.pricingClose}>×</Text>
              </TouchableOpacity>
            </View>
            {pricingLoading && <ActivityIndicator color="#D42F50" accessibilityLabel="Loading pricing rules" />}
            {!pricingLoading && pricingError ? <>
              <Text style={styles.addressError} accessibilityRole="alert">{pricingError}</Text>
              <TouchableOpacity accessibilityRole="button" onPress={() => setPricingRetry((value) => value + 1)}>
                <Text style={styles.pricingRetry}>Retry loading pricing rules</Text>
              </TouchableOpacity>
            </> : null}
            {pricingRules && !pricingLoading && !pricingError && <>
              <View style={styles.pricingRow}><Text style={styles.pricingName}>Delivery</Text><Text style={styles.pricingAmount}>${Number(pricingRules.deliveryFeePerKm).toFixed(2)} / km</Text></View>
              <View style={styles.pricingRow}><Text style={styles.pricingName}>Service fee</Text><Text style={styles.pricingAmount}>${Number(pricingRules.serviceFee).toFixed(2)} per order</Text></View>
              <View style={styles.pricingRow}><Text style={styles.pricingName}>Small-order surcharge</Text><Text style={styles.pricingAmount}>${Number(pricingRules.smallOrderSurcharge).toFixed(2)}</Text></View>
              <Text style={styles.pricingNote}>Applies when a restaurant's items subtotal is below ${Number(pricingRules.smallOrderThreshold).toFixed(2)}.</Text>
              <Text style={styles.pricingNote}>Delivery uses the straight-line distance on the map, rounded to 0.01 km. Each restaurant order is charged separately.</Text>
            </>}
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}
