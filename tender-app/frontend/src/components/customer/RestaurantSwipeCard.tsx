import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Animated, Easing, Image, PanResponder, Platform, Pressable, Text, View, useWindowDimensions, type ViewStyle,
} from 'react-native';
import type { CatalogItem } from '@/api/customerCatalog';
import type { Restaurant } from '@/types/restaurant';
import { formatAverageRating } from '@/app/customer/services/orderRating';
import { styles } from './discover.styles';

const swipeSurface = Platform.OS === 'web'
  ? { touchAction: 'pan-y', userSelect: 'none' } as ViewStyle
  : undefined;
const logo = require('../../../assets/tender-logo.png');
const useNativeDriver = Platform.OS !== 'web';
const easeOut = Easing.out(Easing.cubic);
const flipEase = Easing.bezier(0.38, 0.12, 0.30, 1);
const horizontalIntentRatio = 1.35;

function resistDrag(offset: number, freeTravel: number, limit: number) {
  const distance = Math.abs(offset);
  if (distance <= freeTravel) return offset;
  const overshoot = distance - freeTravel;
  const resistedTravel = limit - freeTravel;
  // Match the initial drag speed at the boundary, then ease toward the limit.
  return Math.sign(offset) * (freeTravel + resistedTravel * overshoot / (resistedTravel + overshoot));
}

function createSwipeResponder({ enabled, threshold, onDrag, onDecision, onCancel }: {
  enabled: boolean;
  threshold: number;
  onDrag: (x: number, y: number) => void;
  onDecision: (saved: boolean) => void;
  onCancel: () => void;
}) {
  // PanResponder zeroes dx and dy on grant. Keep the movement that claimed the gesture.
  let initialX = 0;
  let initialY = 0;
  let lastMoveAt = 0;

  return PanResponder.create({
    onMoveShouldSetPanResponder: (_, { dx, dy, numberActiveTouches }) => {
      if (!enabled || numberActiveTouches !== 1 || Math.abs(dx) < 8 || Math.abs(dx) <= Math.abs(dy) * horizontalIntentRatio) return false;
      initialX = dx;
      initialY = dy;
      lastMoveAt = 0;
      return true;
    },
    onPanResponderGrant: () => onDrag(initialX, initialY),
    onPanResponderMove: (_, { dx, dy }) => {
      lastMoveAt = Date.now();
      onDrag(initialX + dx, initialY + dy);
    },
    onPanResponderRelease: (_, { dx, dy, vx }) => {
      if (!enabled) return;
      const x = initialX + dx;
      const distance = Math.abs(x);
      const forwardVelocity = Math.sign(x) * vx;
      const isFlick = Date.now() - lastMoveAt <= 90 && distance >= threshold * 0.75
        && forwardVelocity >= 0.75 && distance + forwardVelocity * 45 >= threshold;
      if (distance > Math.abs(initialY + dy) * horizontalIntentRatio && (distance >= threshold || isFlick)) onDecision(x > 0);
      else onCancel();
    },
    onPanResponderTerminate: () => {
      if (enabled) onCancel();
    },
  });
}

export function DishPhoto({ item, restaurant, compact = false }: {
  item: CatalogItem;
  restaurant: Restaurant;
  compact?: boolean;
}) {
  const [failedUrls, setFailedUrls] = useState<string[]>([]);
  const url = [item.photURL, restaurant.photoUrl].find((value) => value && !failedUrls.includes(value));

  return (
    <View style={[styles.photo, compact && styles.compactPhoto]}>
      {url ? (
        <Image
          key={url}
          source={{ uri: url }}
          style={styles.photoImage}
          resizeMode="cover"
          fadeDuration={0}
          accessible={false}
          onError={() => setFailedUrls((current) => [...current, url])}
        />
      ) : (
        <Image source={logo} style={styles.photoFallback} resizeMode="contain" accessible={false} />
      )}
    </View>
  );
}

export function RestaurantRating({ restaurant }: { restaurant: Restaurant }) {
  const stars = restaurant.averageRating;
  const count = restaurant.ratingCount;
  return (
    <Text style={styles.rating}>
      {stars != null && !!count && <Text style={styles.star}>★ </Text>}
      {formatAverageRating(stars, count)}
    </Text>
  );
}

export default function RestaurantSwipeCard({ restaurant, featured, previewItems, hasNextCard, reduceMotion, onDecision }: {
  restaurant: Restaurant;
  featured: CatalogItem;
  previewItems: CatalogItem[];
  hasNextCard: boolean;
  reduceMotion: boolean;
  onDecision: (restaurantId: number, saved: boolean) => void;
}) {
  const [preview, setPreview] = useState(false);
  const [previewHeight, setPreviewHeight] = useState(0);
  const [pendingDecision, setPendingDecision] = useState<boolean | null>(null);
  const [position] = useState(() => new Animated.ValueXY());
  const { x: horizontal, y: vertical } = position;
  const [arrival] = useState(() => new Animated.Value(reduceMotion ? 1 : 0));
  const [departure] = useState(() => new Animated.Value(1));
  const [turn] = useState(() => new Animated.Value(0));
  const { width } = useWindowDimensions();
  const swipeThreshold = Math.max(56, Math.min(118, width * 0.30));
  const travelLimit = Math.min(width, 540) + 70;
  const freeHorizontalTravel = Math.min(swipeThreshold * 2, travelLimit * 0.6);
  const leaving = pendingDecision !== null;

  useEffect(() => () => position.stopAnimation(), [position]);

  useEffect(() => {
    if (reduceMotion) {
      arrival.setValue(1);
      return;
    }
    const enter = Animated.timing(arrival, {
      toValue: 1, duration: 185, easing: easeOut, useNativeDriver, isInteraction: false,
    });
    enter.start();
    return () => enter.stop();
  }, [arrival, reduceMotion]);

  useEffect(() => {
    if (pendingDecision === null) return;
    if (reduceMotion) {
      onDecision(restaurant.id, pendingDecision);
      return;
    }
    const exit = Animated.parallel([
      Animated.timing(position, {
        toValue: { x: pendingDecision ? travelLimit : -travelLimit, y: 0 },
        duration: 215, easing: Easing.out(Easing.quad), useNativeDriver,
      }),
      Animated.timing(departure, { toValue: 0, duration: 215, easing: Easing.in(Easing.quad), useNativeDriver }),
    ]);
    exit.start(({ finished }) => {
      if (finished) onDecision(restaurant.id, pendingDecision);
    });
    return () => exit.stop();
  }, [departure, onDecision, pendingDecision, position, reduceMotion, restaurant.id, travelLimit]);

  const decide = useCallback((saved: boolean) => setPendingDecision((current) => current ?? saved), []);

  const resetPosition = useCallback(() => {
    if (reduceMotion) position.setValue({ x: 0, y: 0 });
    else Animated.spring(position, {
      toValue: { x: 0, y: 0 }, stiffness: 420, damping: 38, mass: 0.9, useNativeDriver, isInteraction: false,
    }).start();
  }, [position, reduceMotion]);

  const swipe = useMemo(() => createSwipeResponder({
    enabled: !leaving,
    threshold: swipeThreshold,
    onDrag: (x, y) => {
      if (reduceMotion || leaving) return;
      position.setValue({
        x: resistDrag(x, freeHorizontalTravel, travelLimit),
        y: resistDrag(y * 0.5, 8, 28),
      });
    },
    onDecision: decide,
    onCancel: resetPosition,
  }), [decide, freeHorizontalTravel, leaving, position, reduceMotion, resetPosition, swipeThreshold, travelLimit]);

  useEffect(() => {
    const target = preview ? 1 : 0;
    if (reduceMotion) {
      turn.setValue(target);
      return;
    }
    const transition = Animated.timing(turn, {
      toValue: target, duration: 335, easing: flipEase, useNativeDriver, isInteraction: false,
    });
    transition.start();
    return () => transition.stop();
  }, [preview, reduceMotion, turn]);

  const tilt = horizontal.interpolate({ inputRange: [-180, 0, 180], outputRange: ['-6deg', '0deg', '6deg'], extrapolate: 'clamp' });
  const lift = horizontal.interpolate({ inputRange: [-swipeThreshold, 0, swipeThreshold], outputRange: [0, 7, 0], extrapolate: 'clamp' });
  const backingScale = horizontal.interpolate({ inputRange: [-swipeThreshold, 0, swipeThreshold], outputRange: [1, 0.96, 1], extrapolate: 'clamp' });
  const entryOffset = arrival.interpolate({ inputRange: [0, 1], outputRange: [7, 0] });

  return (
    <View style={styles.deck}>
      <View style={styles.cardStack}>
        {hasNextCard && !reduceMotion && (
          <Animated.View
            pointerEvents="none"
            aria-hidden
            style={[styles.cardBacking, { transform: [{ translateY: lift }, { scale: backingScale }] }]}
          />
        )}
        <Animated.View style={{
          opacity: Animated.multiply(arrival, departure),
          transform: [
            { translateX: horizontal },
            { translateY: Animated.add(vertical, entryOffset) },
            { rotate: tilt },
          ],
        }}>
          <View style={[swipeSurface, { minHeight: previewHeight }]} {...swipe.panHandlers}>
            <Animated.View
              aria-hidden={preview}
              pointerEvents={preview ? 'none' : 'auto'}
              style={[styles.cardFace, { minHeight: previewHeight }, reduceMotion ? { opacity: preview ? 0 : 1 } : {
                opacity: turn.interpolate({ inputRange: [0, 0.49, 0.5, 1], outputRange: [1, 1, 0, 0] }),
                transform: [
                  { perspective: 1250 },
                  { rotateY: turn.interpolate({ inputRange: [0, 0.5, 1], outputRange: ['0deg', '90deg', '90deg'] }) },
                ],
              }]}
            >
              <DishPhoto item={featured} restaurant={restaurant} />
              <View style={styles.cardBody}>
                <Text style={styles.restaurantName}>{restaurant.name}</Text>
                <View style={styles.itemTitleRow}>
                  <Text style={styles.dishName} accessibilityRole="header">{featured.name}</Text>
                  <Text style={styles.price}>${featured.price.toFixed(2)}</Text>
                </View>
                <RestaurantRating restaurant={restaurant} />
              </View>
            </Animated.View>
            <Animated.View
              aria-hidden={!preview}
              pointerEvents={preview ? 'auto' : 'none'}
              style={[styles.cardFace, styles.previewFace, reduceMotion ? { opacity: preview ? 1 : 0 } : {
                opacity: turn.interpolate({ inputRange: [0, 0.5, 0.51, 1], outputRange: [0, 0, 1, 1] }),
                transform: [
                  { perspective: 1250 },
                  { rotateY: turn.interpolate({ inputRange: [0, 0.5, 1], outputRange: ['-90deg', '-90deg', '0deg'] }) },
                ],
              }]}
            >
              <View style={styles.preview} onLayout={(event) => setPreviewHeight(event.nativeEvent.layout.height)}>
                <Text style={styles.previewHeading} accessibilityRole="header">On the menu</Text>
                <Text style={styles.restaurantName}>{restaurant.name}</Text>
                <View style={styles.previewItems}>
                  {previewItems.map((item) => (
                    <View key={item.id} style={styles.previewItem}>
                      <View style={styles.itemTitleRow}>
                        <Text style={styles.previewItemName}>{item.name}</Text>
                        <Text style={styles.price}>${item.price.toFixed(2)}</Text>
                      </View>
                      {!!item.description && <Text numberOfLines={2} style={styles.description}>{item.description}</Text>}
                    </View>
                  ))}
                </View>
              </View>
            </Animated.View>
            <Animated.View pointerEvents="none" accessible={false} aria-hidden style={[styles.swipeHint, styles.skipHint, {
              opacity: horizontal.interpolate({ inputRange: [-swipeThreshold, -20], outputRange: [1, 0], extrapolate: 'clamp' }),
            }]}><Text style={styles.hintText}>Skip</Text></Animated.View>
            <Animated.View pointerEvents="none" accessible={false} aria-hidden style={[styles.swipeHint, styles.saveHint, {
              opacity: horizontal.interpolate({ inputRange: [20, swipeThreshold], outputRange: [0, 1], extrapolate: 'clamp' }),
            }]}><Text style={styles.hintText}>Save</Text></Animated.View>
          </View>
          <Pressable
            onPress={() => setPreview((current) => !current)}
            disabled={leaving}
            accessibilityRole="button"
            accessibilityState={{ expanded: preview, disabled: leaving }}
            style={({ pressed }) => [styles.previewButton, pressed && styles.pressed]}
          >
            <Text style={styles.linkText}>{preview ? 'Back to featured dish' : 'Menu preview'}</Text>
            <Text style={styles.linkArrow} aria-hidden>{preview ? '‹' : '›'}</Text>
          </Pressable>
        </Animated.View>
      </View>
      <View style={styles.actions}>
        <Pressable onPress={() => decide(false)} disabled={leaving} accessibilityRole="button" style={({ pressed }) => [styles.secondaryButton, styles.actionButton, pressed && styles.pressed]}>
          <Text style={styles.secondaryButtonText}><Text aria-hidden>×  </Text>Skip</Text>
        </Pressable>
        <Pressable onPress={() => decide(true)} disabled={leaving} accessibilityRole="button" style={({ pressed }) => [styles.primaryButton, styles.actionButton, pressed && styles.pressed]}>
          <Text style={styles.primaryButtonText}><Text aria-hidden>♡  </Text>Save</Text>
        </Pressable>
      </View>
    </View>
  );
}
