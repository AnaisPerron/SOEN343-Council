import { useState } from 'react';
import {
  Image,
  type ImageSourcePropType,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import type { Restaurant } from '../../../types/restaurant';
import { styles } from './RestaurantCard.styles';

export interface RestaurantCardData {
  restaurant: Restaurant;
  // Optional until the restaurant API exposes ratings.
  rating?: number;
  reviewCount?: number;
  coverOptions: ImageSourcePropType[];
}

interface RestaurantCardProps extends RestaurantCardData {
  onPress: () => void;
}

export default function RestaurantCard({
  restaurant,
  rating,
  reviewCount,
  coverOptions,
  onPress,
}: RestaurantCardProps) {
  // Choose once per mounted card so scrolling or rerendering does not reshuffle covers.
  const [cover] = useState<ImageSourcePropType | undefined>(() =>
    coverOptions.length > 0
      ? coverOptions[Math.floor(Math.random() * coverOptions.length)]
      : undefined,
  );
  const isOpen = restaurant.status === 'open';
  const statusText = isOpen
    ? 'Open now'
    : restaurant.status === 'paused'
      ? 'Taking a short break'
      : 'Closed for now';
  const statusStyle = isOpen
    ? styles.statusOpen
    : restaurant.status === 'paused'
      ? styles.statusPaused
      : styles.statusClosed;

  return (
    <TouchableOpacity
      onPress={onPress}
      style={styles.card}
      accessibilityRole="button"
      accessibilityLabel={`${restaurant.name}, ${restaurant.status}`}
    >
      {/* Keep a square cover frame and retain the branded fallback if no photo exists. */}
      <View style={styles.art}>
        {cover ? (
          <Image source={cover} style={styles.coverImage} resizeMode="cover" />
        ) : (
          <>
            <View style={styles.artRing}>
              <View style={styles.artCircle}>
                <View style={styles.crossLine} />
                <View style={styles.crossLineVertical} />
              </View>
            </View>
            <View style={styles.artLabel}>
              <Text style={styles.artLabelText}>PHOTO COMING SOON</Text>
            </View>
          </>
        )}
      </View>

      <View style={styles.cardMeta}>
        <View style={styles.cardTitleRow}>
          <Text numberOfLines={1} style={styles.cardTitle}>{restaurant.name}</Text>
        </View>
        <Text style={[styles.cardDetail, statusStyle]}>{statusText}</Text>
        {rating != null && (
          <View style={styles.ratingRow}>
            <Text style={styles.ratingStar}>★</Text>
            <Text style={styles.ratingValue}>{rating.toFixed(1)}</Text>
            <Text style={styles.reviewCount}>({reviewCount ?? 0} reviews)</Text>
          </View>
        )}
      </View>
    </TouchableOpacity>
  );
}
