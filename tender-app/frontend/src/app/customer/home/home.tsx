import { useEffect, useRef, useState } from 'react';
import { useRouter } from 'expo-router';
import {
  Animated,
  Image,
  Platform,
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
import type { Restaurant } from '@/types/restaurant';
import { styles } from './home.styles';
import { searchRestaurants } from '../services/restaurantSearch';
import RestaurantSection from '../components/RestaurantSection';
import CartIconButton from '../components/CartIconButton';
import AccountMenu from '@/components/AccountMenu';
import type { RestaurantCardData } from '../components/RestaurantCard';
import {
  getRestaurantOrderRestriction,
  ORDER_ERROR_FADE_DELAY_MS,
  ORDER_ERROR_FADE_DURATION_MS,
  type RestaurantOrderRestriction,
} from '../services/restaurantOrderRules';

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

const toCardData = (restaurant: Restaurant): RestaurantCardData => ({
  restaurant,
  coverOptions: restaurant.photoUrl ? [{ uri: restaurant.photoUrl }] : [],
});

export default function CustomerHome() {
  const router = useRouter();
  const { lines } = useCart();
  const { restaurants, restaurantsStatus, restaurantsError, reloadRestaurants } = useCatalog();
  const cartCount = lines.reduce((count, line) => count + line.quantity, 0);
  const { width } = useWindowDimensions();
  const compact = width < 600;
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [orderError, setOrderError] = useState<RestaurantOrderRestriction | null>(null);
  const [toastOpacity] = useState(() => new Animated.Value(0));
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const normalizedQuery = searchQuery.trim().toLocaleLowerCase();
  const isMobileSearch = Platform.OS !== 'web' && (isSearchFocused || normalizedQuery.length > 0);
  // Search the full restaurant list so matches are not limited to one curated row.
  const searchResults: RestaurantCardData[] = searchRestaurants(restaurants, normalizedQuery).map(toCardData);
  useEffect(() => () => {
    // Clear the pending dismissal if the home screen unmounts while the alert is visible.
    if (toastTimer.current) clearTimeout(toastTimer.current);
  }, []);

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

  return (
    <SafeAreaView style={styles.page} edges={['top']}>
      <ScrollView
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
          <CartIconButton count={cartCount} onPress={() => router.push('/customer/cart/cart')} />
          <AccountMenu />
        </View>

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
          />
        ) : sections.map((section, index) => {
          const items: RestaurantCardData[] = rotate(restaurants, index).map(toCardData);

          return (
            <RestaurantSection
              key={section.title}
              title={section.title}
              note={section.note}
              items={items}
              compact={compact}
              index={index}
              onRestaurantPress={handleRestaurantPress}
            />
          );
        })}
      </ScrollView>
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
