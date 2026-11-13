import { useEffect, useRef, useState } from 'react';
import { useRouter } from 'expo-router';
import {
  Animated,
  Image,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  TouchableOpacity,
  useWindowDimensions,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useCart } from '@/context/CartContext';
import { useCatalog } from '@/context/CatalogContext';
import { useOrders } from '@/context/OrdersContext';
import type { CustomerOrder } from '../services/orderLifecycle';
import type { Restaurant } from '@/types/restaurant';
import type { MapData } from '@/types/dash';
import { styles } from './home.styles';
import { searchRestaurants } from '../services/restaurantSearch';
import RestaurantSection from '../components/RestaurantSection';
import CartIconButton from '../components/CartIconButton';
import AccountMenu from '@/components/AccountMenu';
import CityMap from '@/components/dash/CityMap';
import type { RestaurantCardData } from '../components/RestaurantCard';
import {
  getRestaurantOrderRestriction,
  ORDER_ERROR_FADE_DELAY_MS,
  ORDER_ERROR_FADE_DURATION_MS,
  type RestaurantOrderRestriction,
} from '../services/restaurantOrderRules';

const SPLIT_MIN_WIDTH = 900;

const tenderLogo = require('../../../../assets/tender-logo.png');
{/*Placeholder for now. TODO: for later phases, must be linked to user profile, map, etc.. */}
const sections = [
  { title: 'Top picks for you', note: 'A little something for every craving' },
  { title: 'Open near me', note: 'Ready when you are' },
  { title: 'For cheap treats', note: 'Good food, easy on the wallet' },
  { title: 'Feeling fancy?', note: 'Make tonight feel like an occasion' },
  { title: 'For your sweet tooth', note: 'Save room for something sweet' },
];

// Rotate the list per section so the placeholder rows do not all start with the same restaurant.
const rotate = <T,>(list: T[], offset: number) =>
  list.length === 0 ? list : [...list.slice(offset % list.length), ...list.slice(0, offset % list.length)];

const toCardData = (restaurant: Restaurant, mapData?: MapData | null): RestaurantCardData => ({
  restaurant,
  coverOptions: restaurant.photoUrl ? [{ uri: restaurant.photoUrl }] : [],
  queueCount: mapData?.restaurants.find((item) => item.id === restaurant.id)?.queueCount,
});

export default function CustomerHome() {
  const router = useRouter();
  const { lines } = useCart();
  const { orders, refreshOrders } = useOrders();
  const { restaurants, restaurantsStatus, restaurantsError, reloadRestaurants } = useCatalog();
  const cartCount = lines.reduce((count, line) => count + line.quantity, 0);
  const { width } = useWindowDimensions();
  // Full screen shows the map with the restaurants in a side panel, like a maps app; too narrow for a panel, it's just the map
  const [mapFullScreen, setMapFullScreen] = useState(false);
  const [mapData, setMapData] = useState<MapData | null>(null);
  const [highlightedKeys, setHighlightedKeys] = useState<string[]>([]);
  const [selectedRestaurant, setSelectedRestaurant] = useState<Restaurant | null>(null);
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null);
  const scrollRef = useRef<React.ComponentRef<typeof ScrollView>>(null);
  const mapOffset = useRef(0);
  const toggleMapFullScreen = () => setMapFullScreen((current) => !current);
  const split = mapFullScreen && width >= SPLIT_MIN_WIDTH;
  const mapOnly = mapFullScreen && !split;
  // The side panel is phone-width, so its contents use the compact layout
  const compact = width < 600 || split;
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [orderError, setOrderError] = useState<RestaurantOrderRestriction | null>(null);
  const [toastOpacity] = useState(() => new Animated.Value(0));
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const normalizedQuery = searchQuery.trim().toLocaleLowerCase();
  const isMobileSearch = Platform.OS !== 'web' && (isSearchFocused || normalizedQuery.length > 0);
  // Search the full restaurant list so matches are not limited to one curated row.
  const searchResults: RestaurantCardData[] = searchRestaurants(restaurants, normalizedQuery).map((item) => toCardData(item, mapData));
  const awaitingOrders = orders.filter((order) => ['placed', 'accepted', 'preparing', 'ready', 'picked_up'].includes(order.status));
  const selectedOrder = awaitingOrders.find((order) => order.id === selectedOrderId);
  const mapOrderKeys = (restaurantId: number, courierId?: number | null) => [
    `restaurant-${restaurantId}`, ...(courierId == null ? [] : [`courier-${courierId}`]),
  ];
  useEffect(() => () => {
    // Clear the pending dismissal if the home screen unmounts while the alert is visible.
    if (toastTimer.current) clearTimeout(toastTimer.current);
  }, []);
  useEffect(() => {
    refreshOrders();
    const timer = setInterval(refreshOrders, 15000);
    return () => clearInterval(timer);
  }, [refreshOrders]);

  const handleRestaurantPress = (restaurant: Restaurant) => {
    const restriction = getRestaurantOrderRestriction(restaurant);
    if (!restriction) {
      router.push({ pathname: '/customer/restaurant/[id]', params: { id: String(restaurant.id) } });
      return;
    }

    setOrderError(restriction);
    if (toastTimer.current) clearTimeout(toastTimer.current);
    toastOpacity.stopAnimation();
    toastOpacity.setValue(1);
    // Leave the error readable briefly, then fade it out within two seconds total.
    toastTimer.current = setTimeout(() => {
      Animated.timing(toastOpacity, {
        toValue: 0,
        duration: ORDER_ERROR_FADE_DURATION_MS,
        useNativeDriver: true,
      }).start(({ finished }) => {
        if (finished) setOrderError(null);
      });
    }, ORDER_ERROR_FADE_DELAY_MS);
  };

  // The map only shows open restaurants, but this keeps the same checks and messages as tapping a card
  const openRestaurantFromMap = (restaurantId: number) => {
    const restaurant = restaurants.find((r) => r.id === restaurantId);
    if (split && restaurant) {
      setSelectedRestaurant(restaurant);
      scrollRef.current?.scrollTo({ y: 0, animated: true });
      return;
    }
    if (restaurant) handleRestaurantPress(restaurant);
    // Not loaded in the list yet: the restaurant page loads it and handles a closed restaurant itself
    else router.push({ pathname: '/customer/restaurant/[id]', params: { id: String(restaurantId) } });
  };

  return (
    <SafeAreaView style={styles.page} edges={['top']}>
      <View style={split ? styles.splitRow : styles.single}>
        {!mapOnly && <ScrollView
          ref={scrollRef}
          style={split && styles.sidePanel}
          stickyHeaderIndices={[isMobileSearch ? 1 : 2]}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={[
            styles.content,
            { paddingHorizontal: compact ? 20 : 48 },
          ]}
        >
          <View style={[styles.topbar, compact && styles.mobileTopbar]}>
            {/* TODO: Restore the delivery location control when location selection is implemented.
            <TouchableOpacity
              style={styles.locationPill}
              accessibilityRole="button"
              accessibilityLabel="Delivery location, Montreal"
            >
              <Text style={styles.pin}>⌖</Text>
              <View>
                <Text style={styles.locationCaption}>DELIVERING TO</Text>
                <Text style={styles.location}>Montreal, QC <Text style={styles.chevron}>⌄</Text></Text>
              </View>
            </TouchableOpacity>
            */}
            <TouchableOpacity accessibilityRole="button" accessibilityLabel="Go to city map" style={styles.mapJump} onPress={() => mapFullScreen ? setMapFullScreen(false) : scrollRef.current?.scrollTo({ y: mapOffset.current, animated: true })}>
              <Text style={styles.mapJumpIcon}>⌖</Text>
            </TouchableOpacity>
            <CartIconButton count={cartCount} onPress={() => router.push('/customer/cart/cart')} />
            <AccountMenu />
          </View>

          {split && selectedRestaurant && <View style={styles.selectedRestaurant}>
            <Text style={styles.selectedEyebrow}>SELECTED ON MAP</Text>
            <Text style={styles.selectedTitle}>{selectedRestaurant.name}</Text>
            <Text style={styles.selectedMeta}>Queue Length: {mapData?.restaurants.find((item) => item.id === selectedRestaurant.id)?.queueCount ?? '—'}</Text>
            <TouchableOpacity accessibilityRole="button" onPress={() => handleRestaurantPress(selectedRestaurant)} style={styles.selectedAction}><Text style={styles.selectedActionText}>View menu</Text></TouchableOpacity>
          </View>}

          {!isMobileSearch && (
            <View style={styles.pointsPanel}>
            <View style={styles.pointsTop}>
              <View style={styles.pointsCopy}>
                <Text style={styles.pointsEyebrow}>TENDER CASH</Text>
                {/* Placeholder for now. TODO: Connect with actual Tender Cash */}
                <Text style={styles.pointsValue}>0<Text style={styles.pointsUnit}> pts</Text></Text>
                <Text style={styles.pointsTitle}>Your cheapest way to get lucky tonight.</Text>
              </View>
              {/* Disable the native fade while keeping tintColor consistent across phone and web. */}
              <Image
                source={tenderLogo}
                style={styles.pointsLogo}
                resizeMode="contain"
                fadeDuration={0}
                resizeMethod="resize"
                accessibilityLabel="Tender logo"
              />
            </View>
            <TouchableOpacity style={styles.pointsAction} accessibilityRole="button">
              {/* Empty Route for now. TODO: should lead to the swiping tinder-like page */}
              <Text style={styles.pointsActionText}>Find a yummy soulmate</Text>
              <View style={styles.pointsActionIcon}>
                <Text style={styles.pointsActionArrow}>›</Text>
              </View>
            </TouchableOpacity>
            </View>
          )}

          {/* Keep the search background opaque while rows scroll underneath it. */}
          <View style={styles.stickySearch}>
            <View style={styles.search}>
              <Text style={styles.searchIcon}>{'\u2315'}</Text>
              <TextInput
                value={searchQuery}
                onChangeText={setSearchQuery}
                placeholder="Search restaurants by name"
                placeholderTextColor="#A99498"
                style={styles.searchInput}
                accessibilityLabel="Search restaurants by name"
                onFocus={() => setIsSearchFocused(true)}
                onBlur={() => setIsSearchFocused(false)}
                underlineColorAndroid="transparent"
                selectionColor="#FF4458"
                returnKeyType="search"
                autoCapitalize="none"
                autoCorrect={false}
              />
              {searchQuery.length > 0 && (
                <TouchableOpacity
                  onPress={() => setSearchQuery('')}
                  accessibilityRole="button"
                  accessibilityLabel="Clear restaurant search"
                  style={styles.clearSearch}
                >
                  <Text style={styles.clearSearchText}>x</Text>
                </TouchableOpacity>
              )}
            </View>
          </View>


          {restaurantsStatus === 'loading' ? (
            <Text style={styles.catalogStatus}>Loading restaurants...</Text>
          ) : restaurantsStatus === 'error' ? (
            <View>
              <Text style={styles.catalogStatus}>{restaurantsError}</Text>
              <TouchableOpacity onPress={reloadRestaurants} accessibilityRole="button">
                <Text style={styles.retryText}>Try again</Text>
              </TouchableOpacity>
            </View>
          ) : restaurants.length === 0 ? (
            <Text style={styles.catalogStatus}>No restaurants are open right now.</Text>
          ) : normalizedQuery ? (
            <RestaurantSection
              title="Search results"
              note={`${searchResults.length} ${searchResults.length === 1 ? 'restaurant' : 'restaurants'}`}
              items={searchResults}
              compact={compact}
              index={0}
              onRestaurantPress={handleRestaurantPress}
              onRestaurantHover={(id) => setHighlightedKeys(id == null ? [] : [`restaurant-${id}`])}
            />
          ) : sections.map((section, index) => {
            const items: RestaurantCardData[] = rotate(restaurants, index).map((item) => toCardData(item, mapData));

            return (
              <RestaurantSection
                key={section.title}
                title={section.title}
                note={section.note}
                items={items}
                compact={compact}
                index={index}
                onRestaurantPress={handleRestaurantPress}
                onRestaurantHover={(id) => setHighlightedKeys(id == null ? [] : [`restaurant-${id}`])}
              />
            );
          })}

          {!mapFullScreen && (
            <View style={styles.cityMap} onLayout={(event) => { mapOffset.current = event.nativeEvent.layout.y; }}>
              <CityMap onToggleFullScreen={toggleMapFullScreen} onRestaurantPress={openRestaurantFromMap} highlightedKeys={highlightedKeys} onData={setMapData} />
              <AwaitingOrders orders={awaitingOrders} mapData={mapData} onHover={setHighlightedKeys} onSelect={setSelectedOrderId} mapOrderKeys={mapOrderKeys} />
            </View>
          )}
        </ScrollView>}
        {mapFullScreen && (
          <View style={styles.mapPane}>
            <CityMap fill onToggleFullScreen={toggleMapFullScreen} onRestaurantPress={openRestaurantFromMap} highlightedKeys={highlightedKeys} onData={setMapData} />
            {split && <AwaitingOrders orders={awaitingOrders} mapData={mapData} onHover={setHighlightedKeys} onSelect={setSelectedOrderId} mapOrderKeys={mapOrderKeys} />}
          </View>
        )}
      </View>
      <Modal visible={!!selectedOrder} transparent animationType="fade" onRequestClose={() => setSelectedOrderId(null)}>
        <View style={styles.orderModalBackdrop}><View style={styles.orderModal}>
          <Text style={styles.selectedEyebrow}>ORDER SUMMARY</Text>
          <Text style={styles.selectedTitle}>Order #{selectedOrder?.id}</Text>
          <Text style={styles.selectedMeta}>{selectedOrder?.restaurantName} · {selectedOrder?.status.replace('_', ' ')}</Text>
          {selectedOrder?.items?.map((item, index) => <Text key={index} style={styles.selectedMeta}>{item.quantity} × {item.name}</Text>)}
          <Text style={styles.selectedMeta}>Total ${selectedOrder?.total.toFixed(2)}</Text>
          <TouchableOpacity accessibilityRole="button" onPress={() => setSelectedOrderId(null)} style={styles.selectedAction}><Text style={styles.selectedActionText}>Close</Text></TouchableOpacity>
        </View></View>
      </Modal>
      {orderError && (
        <View style={styles.orderErrorOverlay} pointerEvents="none">
          <Animated.View
            style={[styles.orderErrorToast, { opacity: toastOpacity }]}
            accessibilityRole="alert"
            accessibilityLiveRegion="assertive"
          >
            <Text style={styles.orderErrorCode}>{orderError.code}</Text>
            <Text style={styles.orderErrorMessage}>{orderError.message}</Text>
          </Animated.View>
        </View>
      )}
    </SafeAreaView>
  );
}

function AwaitingOrders({ orders, mapData, onHover, onSelect, mapOrderKeys }: {
  orders: CustomerOrder[];
  mapData: MapData | null;
  onHover: (keys: string[]) => void;
  onSelect: (id: string) => void;
  mapOrderKeys: (restaurantId: number, courierId?: number | null) => string[];
}) {
  if (orders.length === 0) return null;
  return <View style={styles.awaitingPanel}>
    <Text style={styles.awaitingTitle}>Awaiting orders</Text>
    {orders.map((order) => {
      const courier = mapData?.couriers.find((item) => item.courierId === order.courierId);
      return <Pressable key={order.id} accessibilityRole="button" onPress={() => onSelect(order.id)}
        onHoverIn={() => onHover(mapOrderKeys(order.restaurantId, order.courierId))} onHoverOut={() => onHover([])} style={styles.awaitingRow}>
        <View style={styles.awaitingColumn}><Text style={styles.awaitingCaption}>COURIER ON THE WAY</Text><Text style={styles.awaitingName}>{courier?.name ?? (order.courierId ? `Courier #${order.courierId}` : 'Awaiting assignment')}</Text></View>
        <View style={styles.awaitingColumn}><Text style={styles.awaitingCaption}>RESTAURANT</Text><Text style={styles.awaitingName}>{order.restaurantName ?? `Restaurant #${order.restaurantId}`}</Text></View>
        <Text style={styles.awaitingNumber}>#{order.id}</Text>
      </Pressable>;
    })}
  </View>;
}
