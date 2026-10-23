import { useEffect, useRef, useState } from 'react';
import { Animated, Text, TouchableOpacity, useWindowDimensions, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { menuItemOptionsMock, menuItemsMock, menusMock } from '../../../mocks/menus.mock';
import { menuItemPhotos } from '../../../mocks/restaurantDisplay.mock';
import { restaurantsMock } from '../../../mocks/restaurants.mock';
import { useCart } from '../../../context/CartContext';
import { useItemCustomization } from '../../../context/ItemCustomizationContext';
import { DetailBackButton, DetailPage } from '../components/DetailPage';
import { getMenuItemOrderRestriction, getRestaurantOrderRestriction, ORDER_ERROR_FADE_DELAY_MS, ORDER_ERROR_FADE_DURATION_MS, type RestaurantOrderRestriction } from '../restaurantOrderRules';
import { styles } from './item.styles';

export default function MenuItemDetails() {
  const { id } = useLocalSearchParams<{ id: string }>();
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
  const item = menuItemsMock.find((entry) => entry.id === Number(id));
  const selectedOptionIds = selections[item?.id ?? -1] ?? [];
  const menu = menusMock.find((entry) => entry.id === item?.menu_id);
  const restaurant = restaurantsMock.find((entry) => entry.id === menu?.restaurant_id);
  const options = menuItemOptionsMock.filter((option) => option.menu_item_id === item?.id);
  // The displayed total includes selected extras for every unit.
  const optionCost = options.reduce((total, option) => total + (selectedOptionIds.includes(option.id) ? option.additional_cost : 0), 0);
  const total = ((item?.price ?? 0) + optionCost) * quantity;
  const cartCount = lines.reduce((count, line) => count + line.quantity, 0);
  const coverHeight = Math.max(290, Math.min(windowHeight * 0.39, 390));

  useEffect(() => () => {
    if (toastTimer.current) clearTimeout(toastTimer.current);
  }, []);

  if (!item || !restaurant) {
    return <View style={styles.notFound}>
      <Text style={styles.title}>Menu item not found</Text>
      <TouchableOpacity onPress={() => router.replace('/customer/home')} accessibilityRole="button">
        <Text style={styles.backLink}>Back to restaurants</Text>
      </TouchableOpacity>
    </View>;
  }

  const cover = item.photURL ? { uri: item.photURL } : menuItemPhotos[item.id];
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
    // Check both restrictions here so a direct link cannot add an unavailable item or a closed restaurant's item.
    const restriction = getRestaurantOrderRestriction(restaurant) ?? getMenuItemOrderRestriction(item);
    if (restriction) return showError(restriction);
    addItem(item, restaurant.id, selectedOptionIds, optionCost, quantity);
    router.push('/customer/cart');
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
          <TouchableOpacity style={styles.coverCart} accessibilityRole="button" accessibilityLabel={`Cart, ${cartCount} items`} onPress={() => router.push('/customer/cart')}><Text style={styles.coverCartText}>Cart{cartCount > 0 ? ` (${cartCount})` : ''}</Text></TouchableOpacity>
        </View>}
        {stickyVisible && <View style={[styles.stickyHeader, { paddingTop: insets.top }]}>
          <DetailBackButton onPress={goBack} />
          <Text numberOfLines={1} style={styles.stickyTitle}>{item.name}</Text>
          <TouchableOpacity style={styles.stickyCart} accessibilityRole="button" accessibilityLabel={`Cart, ${cartCount} items`} onPress={() => router.push('/customer/cart')}><Text style={styles.stickyCartText}>Cart{cartCount > 0 ? ` (${cartCount})` : ''}</Text></TouchableOpacity>
        </View>}
        <View style={[styles.bottomBar, { paddingBottom: Math.max(insets.bottom, 12) }]}>
          <TouchableOpacity style={[styles.addToCart, (!item.available || restaurant.status !== 'open') && styles.addToCartUnavailable]} accessibilityRole="button" accessibilityLabel={`Add ${quantity} ${item.name} to cart for $${total.toFixed(2)}`} onPress={addToCart}>
            <Text style={styles.addToCartText}>+  Add to Cart · ${total.toFixed(2)}</Text>
          </TouchableOpacity>
        </View>
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
          <TouchableOpacity style={styles.customizeButton} accessibilityRole="button" onPress={() => router.push({ pathname: '/customer/customize/[id]', params: { id: String(item.id) } })}>
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
