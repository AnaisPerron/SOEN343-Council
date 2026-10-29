import { useRef } from 'react';
import { Platform, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import type { Restaurant } from '../../../types/restaurant';
import RestaurantCard, { type RestaurantCardData } from './RestaurantCard';
import { styles } from './RestaurantSection.styles';

interface RestaurantSectionProps {
  title: string;
  note: string;
  items: RestaurantCardData[];
  compact: boolean;
  index: number;
  onRestaurantPress: (restaurant: Restaurant) => void;
}

export default function RestaurantSection({
  title,
  note,
  items,
  compact,
  index,
  onRestaurantPress,
}: RestaurantSectionProps) {
  const scrollRef = useRef<React.ComponentRef<typeof ScrollView>>(null);
  const scrollBy = (direction: number) => {
    scrollRef.current?.scrollTo({ x: direction * 430, animated: true });
  };

  return (
    <View style={styles.section}>
      <View style={styles.sectionHeader}>
        <View style={styles.sectionTitles}>
          <Text style={styles.sectionTitle}>{title}</Text>
          <Text style={styles.sectionNote}>{note}</Text>
        </View>
        {Platform.OS === 'web' && !compact && (
          <View style={styles.arrows}>
            <TouchableOpacity
              onPress={() => scrollBy(-1)}
              style={styles.arrow}
              accessibilityRole="button"
              accessibilityLabel={`Scroll ${title} left`}
            >
              <Text style={styles.arrowText}>‹</Text>
            </TouchableOpacity>
            <TouchableOpacity
              onPress={() => scrollBy(1)}
              style={styles.arrow}
              accessibilityRole="button"
              accessibilityLabel={`Scroll ${title} right`}
            >
              <Text style={styles.arrowText}>›</Text>
            </TouchableOpacity>
          </View>
        )}
      </View>

      {items.length === 0 ? (
        <Text style={styles.emptySearch}>No restaurants match that name.</Text>
      ) : (
        <ScrollView
          ref={scrollRef}
          horizontal
          showsHorizontalScrollIndicator={Platform.OS === 'web'}
          contentContainerStyle={styles.cardRow}
        >
          {items.map((item, itemIndex) => (
            <RestaurantCard
              key={`${index}-${item.restaurant.id}-${itemIndex}`}
              {...item}
              onPress={() => onRestaurantPress(item.restaurant)}
            />
          ))}
        </ScrollView>
      )}
    </View>
  );
}
