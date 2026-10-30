import { useEffect, useRef, useState } from 'react';
import { Animated, Text, TouchableOpacity, useWindowDimensions, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useCart } from '../../../context/CartContext';
import { useCatalog, useRestaurantMenu } from '../../../context/CatalogContext';
import { useItemCustomization } from '../../../context/ItemCustomizationContext';
import { DetailBackButton, DetailPage } from '../components/DetailPage';
import CartIconButton from '../components/CartIconButton';
import { getMenuItemOrderRestriction, getRestaurantOrderRestriction, ORDER_ERROR_FADE_DELAY_MS, ORDER_ERROR_FADE_DURATION_MS, type RestaurantOrderRestriction } from '../services/restaurantOrderRules';
import { styles } from './item.styles';

export default function MenuItemDetails() {
  const { id, restaurantId } = useLocalSearchParams<{ id: string; restaurantId?: string }>();
  const router = useRouter();
  const { lines, addItem } = useCart();
  const { selections } = useItemCustomization();
  const insets = useSafeAreaInsets();
  const { height: windowHeight } = useWindowDimensions();
  const [stickyVisible, setStickyVisible] = useState(false);
  const [quantity, setQuantity] = useState(1);
  const [orderError, setOrderError] = useState<RestaurantOrderRestriction | null>(null);
  const [toastOpacity] = useState(() => new Animated.Value(0));
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [addedCount, setAddedCount] = useState<number | null>(null);
  const [isAdding, setIsAdding] = useState(false);
  const addingRef = useRef(false);
  const [addedOpacity] = useState(() => new Animated.Value(0));
  const [addedOffset] = useState(() => new Animated.Value(12));
  const { restaurants, findItem } = useCatalog();
  // The route carries the restaurant so a refreshed or linked page can load the right menu.
  const { menu, status: menuStatus } = useRestaurantMenu(restaurantId ? Number(restaurantId) : findItem(Number(id))?.restaurantId);
  const item = menu?.items.find((entry) => entry.id === Number(id)) ?? findItem(Number(id));
  const selectedOptionIds = selections[item?.id ?? -1] ?? [];
  const restaurant = restaurants.find((entry) => entry.id === item?.restaurantId);
  const options = (menu?.options ?? []).filter((option) => option.menu_item_id === item?.id);
  // The displayed total includes selected extras for every unit.
  const optionCost = options.reduce((total, option) => total + (selectedOptionIds.includes(option.id) ? option.additional_cost : 0), 0);
  const total = ((item?.price ?? 0) + optionCost) * quantity;
  const cartCount = lines.reduce((count, line) => count + line.quantity, 0);
  const coverHeight = Math.max(290, Math.min(windowHeight * 0.39, 390));

  useEffect(() => () => {
    if (toastTimer.current) clearTimeout(toastTimer.current);
    addedOpacity.stopAnimation();
    addedOffset.stopAnimation();
  }, [addedOpacity, addedOffset]);

  if ((!item || !restaurant) && menuStatus === 'loading') {
    return <View style={styles.notFound}><Text style={styles.description}>Loading menu item...</Text></View>;
  }

  if (!item || !restaurant) {
    return <View style={styles.notFound}>
      <Text style={styles.title}>Menu item not found</Text>
      <TouchableOpacity onPress={() => router.replace('/customer/home/home')} accessibilityRole="button">
        <Text style={styles.backLink}>Back to restaurants</Text>
      </TouchableOpacity>
    </View>;
  }

  const cover = item.photURL ? { uri: item.photURL } : undefined;
  const goBack = () => router.canGoBack()
    ? router.back()
    : router.replace({ pathname: '/customer/restaurant/[id]', params: { id: String(restaurant.id) } });

  const showError = (restriction: RestaurantOrderRestriction) => {
    setOrderError(restriction);
    if (toastTimer.current) clearTimeout(toastTimer.current);
    toastOpacity.stopAnimation();
    toastOpacity.setValue(1);
    toastTimer.current = setTimeout(() => {
      Animated.timing(toastOpacity, { toValue: 0, duration: ORDER_ERROR_FADE_DURATION_MS, useNativeDriver: true }).start(({ finished }) => {
        if (finished) setOrderError(null);
      });
    }, ORDER_ERROR_FADE_DELAY_MS);
  };

  const addToCart = () => {
    if (addingRef.current) return;
    // Check both restrictions here so a direct link cannot add an unavailable item or a closed restaurant's item.
    const restriction = getRestaurantOrderRestriction(restaurant) ?? getMenuItemOrderRestriction(item);
    if (restriction) return showError(restriction);
    // Lock immediately so rapid taps cannot create duplicate cart lines before the button rerenders.
    addingRef.current = true;
    addItem(item, restaurant.id, selectedOptionIds, optionCost, quantity);
    setAddedCount(quantity);
    setIsAdding(true);
    addedOpacity.setValue(0);
    addedOffset.setValue(12);
    // Confirm the cart update before returning to this restaurant's menu.
    Animated.sequence([
      Animated.parallel([
        Animated.timing(addedOpacity, { toValue: 1, duration: 180, useNativeDriver: true }),
        Animated.timing(addedOffset, { toValue: 0, duration: 180, useNativeDriver: true }),
      ]),
      Animated.delay(1300),
      Animated.timing(addedOpacity, { toValue: 0, duration: 260, useNativeDriver: true }),
    ]).start(({ finished }) => {
      if (finished) router.replace({ pathname: '/customer/restaurant/[id]', params: { id: String(restaurant.id) } });
    });
  };

  return (
    <DetailPage
      title={item.name}
      cover={cover}
      coverHeight={coverHeight}
      onScroll={(event) => setStickyVisible(event.nativeEvent.contentOffset.y >= coverHeight - 30 - insets.top)}
      foreground={<>
        {!stickyVisible && <View style={[styles.coverControls, { top: insets.top + 12 }]}>
          <DetailBackButton onPress={goBack} />
          <CartIconButton count={cartCount} tone="dark" onPress={() => router.push('/customer/cart/cart')} />
        </View>}
        {stickyVisible && <View style={[styles.stickyHeader, { paddingTop: insets.top }]}>
          <DetailBackButton onPress={goBack} />
          <Text numberOfLines={1} style={styles.stickyTitle}>{item.name}</Text>
          <CartIconButton count={cartCount} onPress={() => router.push('/customer/cart/cart')} />
        </View>}
        <View style={[styles.bottomBar, { paddingBottom: Math.max(insets.bottom, 12) }]}>
          <TouchableOpacity style={[styles.addToCart, (!item.available || restaurant.status !== 'open') && styles.addToCartUnavailable]} accessibilityRole="button" accessibilityLabel={`Add ${quantity} ${item.name} to cart for $${total.toFixed(2)}`} accessibilityState={{ disabled: isAdding }} disabled={isAdding} onPress={addToCart}>
            <Text style={styles.addToCartText}>+  Add to Cart · ${total.toFixed(2)}</Text>
          </TouchableOpacity>
        </View>
        {addedCount !== null && <Animated.View pointerEvents="none" style={[styles.addedToast, { bottom: Math.max(insets.bottom, 12) + 82, opacity: addedOpacity, transform: [{ translateY: addedOffset }] }]} accessibilityRole="alert" accessibilityLiveRegion="polite">
          <View style={styles.addedCheck}><Text style={styles.addedCheckText}>✓</Text></View>
          <Text style={styles.addedText}>{addedCount} {addedCount === 1 ? 'item' : 'items'} added to cart</Text>
        </Animated.View>}
        {orderError && <View style={styles.orderErrorOverlay} pointerEvents="none">
          <Animated.View style={[styles.orderErrorToast, { opacity: toastOpacity }]} accessibilityRole="alert" accessibilityLiveRegion="assertive">
            <Text style={styles.orderErrorCode}>{orderError.code}</Text>
            <Text style={styles.orderErrorMessage}>{orderError.message}</Text>
          </Animated.View>
        </View>}
      </>}
    >
      <View style={styles.itemPanel}>
        <Text style={styles.restaurantName}>{restaurant.name}</Text>
        <Text style={styles.title}>{item.name}</Text>
        <Text style={styles.price}>${item.price.toFixed(2)}</Text>
        <Text style={styles.description}>{item.description}</Text>
        {!item.available && <Text style={styles.unavailable}>Not available right now</Text>}

        <View style={styles.customizeSection}>
          <TouchableOpacity style={styles.customizeButton} accessibilityRole="button" onPress={() => router.push({ pathname: '/customer/customize/[id]', params: { id: String(item.id), restaurantId: String(restaurant.id) } })}>
            <Text style={styles.customizeText}>Customize This Item</Text>
            <Text style={styles.customizeArrow}>›</Text>
          </TouchableOpacity>
          {selectedOptionIds.length > 0 && <Text style={styles.customizeSummary}>{options.filter((option) => selectedOptionIds.includes(option.id)).map((option) => option.name).join(', ')}</Text>}
        </View>

        <View style={styles.quantityControl}>
          <TouchableOpacity style={[styles.quantityButton, quantity === 1 && styles.quantityButtonMinimum]} accessibilityRole="button" accessibilityLabel="Decrease quantity" accessibilityState={{ disabled: quantity === 1 }} disabled={quantity === 1} onPress={() => setQuantity((value) => Math.max(1, value - 1))}><Text style={styles.quantityButtonText}>−</Text></TouchableOpacity>
          <Text style={styles.quantityText}>{quantity}</Text>
          <TouchableOpacity style={styles.quantityButton} accessibilityRole="button" accessibilityLabel="Increase quantity" onPress={() => setQuantity((value) => value + 1)}><Text style={styles.quantityButtonText}>+</Text></TouchableOpacity>
        </View>
      </View>
    </DetailPage>
  );
}
