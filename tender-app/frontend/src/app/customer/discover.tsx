import { useCallback, useEffect, useRef, useState, type ComponentRef, type ReactNode } from 'react';
import { useRouter } from 'expo-router';
import { AccessibilityInfo, ActivityIndicator, Animated, Easing, Image, Platform, Pressable, ScrollView, Text, View, useWindowDimensions } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import type { CatalogItem } from '@/api/customerCatalog';
import RestaurantSwipeCard, { DishPhoto, RestaurantRating } from '@/components/customer/RestaurantSwipeCard';
import { styles } from '@/components/customer/discover.styles';
import { useCart } from '@/context/CartContext';
import { useCatalog, useRestaurantMenu } from '@/context/CatalogContext';
import type { Restaurant } from '@/types/restaurant';
import CartIconButton from './components/CartIconButton';
import { getRestaurantOrderRestriction } from './services/restaurantOrderRules';

const SHORTLIST_SIZE = 3;

type Decision = { restaurantId: number; saved: boolean };
type Pick = { restaurant: Restaurant; featured: CatalogItem };

function featuredDish(items: CatalogItem[] | undefined) {
  return items?.find((item) => item.available && item.photURL) ?? items?.find((item) => item.available);
}

function DiscoveryStatus({ title, detail, loading, children }: {
  title: string;
  detail?: string;
  loading?: boolean;
  children?: ReactNode;
}) {
  return (
    <View style={styles.statusPanel}>
      {loading && <ActivityIndicator color="#D42F50" />}
      <Text style={styles.statusTitle} accessibilityLiveRegion="polite">{title}</Text>
      {detail && <Text style={styles.statusDetail}>{detail}</Text>}
      {children}
    </View>
  );
}

export default function DiscoverRestaurants() {
  const router = useRouter();
  const { restaurants, restaurantsStatus, reloadRestaurants, menus, loadMenu } = useCatalog();
  const { lines } = useCart();
  const { width } = useWindowDimensions();
  const scroll = useRef<ComponentRef<typeof ScrollView>>(null);
  const [decisions, setDecisions] = useState<Decision[]>([]);
  const [reduceMotion, setReduceMotion] = useState(true);
  const [resultsReveal] = useState(() => new Animated.Value(0));
  const wide = width >= 850;
  const cartCount = lines.reduce((count, line) => count + line.quantity, 0);
  const openRestaurants = restaurants.filter((restaurant) => !getRestaurantOrderRestriction(restaurant));
  const picks = decisions.flatMap<Pick>(({ restaurantId, saved }) => {
    if (!saved) return [];
    const restaurant = openRestaurants.find(({ id }) => id === restaurantId);
    const featured = featuredDish(menus[restaurantId]?.menu?.items);
    return restaurant && featured ? [{ restaurant, featured }] : [];
  });
  const candidates = openRestaurants.filter(({ id }) => {
    const entry = menus[id];
    return !decisions.some(({ restaurantId }) => restaurantId === id)
      && (entry?.status !== 'ready' || !!featuredDish(entry.menu?.items));
  });
  const roundComplete = picks.length === SHORTLIST_SIZE;
  const compare = restaurantsStatus === 'ready' && (roundComplete || (!candidates.length && !!picks.length));
  const active = restaurantsStatus === 'ready' && !roundComplete ? candidates[0] : undefined;
  const next = active ? candidates[1] : undefined;
  const nextId = next?.id;
  const nextMenu = nextId === undefined ? undefined : menus[nextId];
  const nextPhoto = featuredDish(nextMenu?.menu?.items)?.photURL ?? next?.photoUrl;
  const { menu, status: menuStatus, retry: retryMenu } = useRestaurantMenu(active?.id);
  const featured = featuredDish(menu?.items);
  const otherDishes = menu?.items.filter((item) => item.available && item.id !== featured?.id).slice(0, 3) ?? [];

  useEffect(() => {
    if (nextId !== undefined && !nextMenu) loadMenu(nextId);
  }, [loadMenu, nextId, nextMenu]);

  useEffect(() => {
    if (nextPhoto) void Image.prefetch(nextPhoto).catch(() => {});
  }, [nextPhoto]);

  useEffect(() => {
    let active = true;
    void AccessibilityInfo.isReduceMotionEnabled().then((enabled) => {
      if (active) setReduceMotion(enabled);
    });
    const subscription = AccessibilityInfo.addEventListener('reduceMotionChanged', setReduceMotion);
    return () => {
      active = false;
      subscription?.remove();
    };
  }, []);

  useEffect(() => {
    if (compare) scroll.current?.scrollTo({ y: 0, animated: false });
  }, [compare]);

  useEffect(() => {
    if (!compare || reduceMotion) {
      resultsReveal.setValue(0);
      return;
    }
    const reveal = Animated.timing(resultsReveal, {
      toValue: 1, duration: 240, easing: Easing.out(Easing.cubic), useNativeDriver: Platform.OS !== 'web',
    });
    reveal.start();
    return () => reveal.stop();
  }, [compare, reduceMotion, resultsReveal]);

  const decide = useCallback((restaurantId: number, saved: boolean) => {
    setDecisions((current) => current.some((decision) => decision.restaurantId === restaurantId)
      ? current
      : [...current, { restaurantId, saved }]);
  }, []);

  const browse = () => router.dismissTo('/customer/home/home');
  const openMenu = (id: number) => router.push({ pathname: '/customer/restaurant/[id]', params: { id: String(id), from: 'discover' } });
  const restart = () => setDecisions([]);
  const refreshDiscovery = () => {
    reloadRestaurants();
    for (const { id } of openRestaurants) {
      const entry = menus[id];
      if (entry?.status === 'ready' && !featuredDish(entry.menu?.items)) loadMenu(id);
    }
  };

  return (
    <SafeAreaView style={styles.page}>
      <ScrollView ref={scroll} contentContainerStyle={[styles.content, wide && styles.wideContent]}>
        <View style={styles.topbar}>
          <Pressable onPress={browse} accessibilityRole="button" style={({ pressed }) => [styles.browseButton, pressed && styles.pressed]}>
            <Text style={styles.browseArrow} aria-hidden>‹</Text>
            <Text style={styles.linkText}>Browse restaurants</Text>
          </Pressable>
          <CartIconButton count={cartCount} onPress={() => router.push('/customer/cart/cart')} />
        </View>

        {compare ? (
          <Animated.View style={[styles.results, !reduceMotion && {
            opacity: resultsReveal,
            transform: [{ translateY: resultsReveal.interpolate({ inputRange: [0, 1], outputRange: [9, 0] }) }],
          }]}>
            <View style={styles.resultIntro}>
              <Text style={styles.heading} accessibilityRole="header" accessibilityLiveRegion="polite">{roundComplete ? 'You have a match!' : 'Your shortlist'}</Text>
              <Text style={[styles.introText, styles.resultIntroText]}>{picks.length === 1 ? 'Take a look at the menu for your saved restaurant.' : 'Compare your picks and open the menu for your favorite.'}</Text>
            </View>
            <View style={[styles.resultsGrid, wide && picks.length > 1 && styles.resultsGridWide]}>
              {picks.map(({ restaurant, featured }) => (
                <View key={restaurant.id} style={styles.resultCard}>
                  <DishPhoto item={featured} restaurant={restaurant} compact />
                  <View style={styles.resultBody}>
                    <Text style={styles.resultRestaurant} accessibilityRole="header">{restaurant.name}</Text>
                    <RestaurantRating restaurant={restaurant} />
                    <View style={styles.resultDish}>
                      <Text style={styles.previewItemName}>{featured.name}</Text>
                      <Text style={styles.price}>${featured.price.toFixed(2)}</Text>
                    </View>
                    <Pressable
                      onPress={() => openMenu(restaurant.id)}
                      accessibilityRole="button"
                      accessibilityLabel={`View menu for ${restaurant.name}`}
                      style={({ pressed }) => [styles.primaryButton, pressed && styles.pressed]}
                    >
                      <Text style={styles.primaryButtonText}>View menu</Text>
                    </Pressable>
                  </View>
                </View>
              ))}
            </View>
            <Pressable onPress={restart} accessibilityRole="button" style={({ pressed }) => [styles.secondaryButton, styles.restart, pressed && styles.pressed]}>
              <Text style={styles.secondaryButtonText}>Start a new round</Text>
            </Pressable>
          </Animated.View>
        ) : (
          <View style={[styles.stage, wide && styles.stageWide]}>
            <View style={[styles.intro, wide && styles.introWide]}>
              <Text style={styles.heading} accessibilityRole="header">Discover restaurants</Text>
              <Text style={styles.introText}>Swipe right to save, left to skip. Save three to compare your favorites.</Text>
              <View style={styles.progress}>
                <View style={styles.progressSegments} aria-hidden>
                  {Array.from({ length: SHORTLIST_SIZE }, (_, index) => (
                    <View key={index} style={[styles.progressSegment, index < picks.length && styles.progressFilled]} />
                  ))}
                </View>
                <Text style={styles.progressText} accessibilityLiveRegion="polite">{picks.length} of {SHORTLIST_SIZE} saved</Text>
              </View>
            </View>
            <View style={styles.deckColumn}>
              {restaurantsStatus === 'loading' ? (
                <DiscoveryStatus title="Finding open restaurants…" loading />
              ) : restaurantsStatus === 'error' ? (
                <DiscoveryStatus title="Restaurants couldn’t load">
                  <Pressable onPress={reloadRestaurants} accessibilityRole="button" style={styles.secondaryButton}>
                    <Text style={styles.secondaryButtonText}>Try again</Text>
                  </Pressable>
                </DiscoveryStatus>
              ) : !active ? (
                <DiscoveryStatus title={decisions.length ? 'You’ve reached the end' : 'No restaurants to discover right now'}
                  detail={decisions.length ? 'Start a new round or browse the full list.' : 'Check back for restaurants with available dishes.'}>
                  <Pressable onPress={decisions.length ? restart : refreshDiscovery} accessibilityRole="button" style={styles.secondaryButton}>
                    <Text style={styles.secondaryButtonText}>{decisions.length ? 'Start a new round' : 'Refresh restaurants'}</Text>
                  </Pressable>
                </DiscoveryStatus>
              ) : menuStatus === 'error' ? (
                <DiscoveryStatus title={`Couldn’t load the menu for ${active.name}`}>
                  <View style={styles.actions}>
                    <Pressable onPress={() => decide(active.id, false)} accessibilityRole="button" style={[styles.secondaryButton, styles.actionButton]}>
                      <Text style={styles.secondaryButtonText}>Skip</Text>
                    </Pressable>
                    <Pressable onPress={retryMenu} accessibilityRole="button" style={[styles.primaryButton, styles.actionButton]}>
                      <Text style={styles.primaryButtonText}>Try again</Text>
                    </Pressable>
                  </View>
                </DiscoveryStatus>
              ) : menuStatus === 'ready' && featured ? (
                <RestaurantSwipeCard
                  key={active.id}
                  restaurant={active}
                  featured={featured}
                  previewItems={otherDishes.length ? otherDishes : [featured]}
                  hasNextCard={nextId !== undefined}
                  reduceMotion={reduceMotion}
                  onDecision={decide}
                />
              ) : (
                <DiscoveryStatus title={`Loading the menu for ${active.name}…`} loading />
              )}
            </View>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
