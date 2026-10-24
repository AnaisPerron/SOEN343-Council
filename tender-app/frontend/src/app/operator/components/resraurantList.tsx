import { Text, TextStyle, View, ViewStyle } from 'react-native';
import type { Restaurant } from '../../../types/restaurant';
import { styles } from './restaurants.styles';

interface RestaurantListProps {
  restaurants: Restaurant[];
}

const CHIP_STYLES: Record<Restaurant['status'], ViewStyle> = {
  open: styles.chipOpen,
  paused: styles.chipPaused,
  closed: styles.chipClosed,
};

const CHIP_TEXT_STYLES: Record<Restaurant['status'], TextStyle> = {
  open: styles.chipTextOpen,
  paused: styles.chipTextPaused,
  closed: styles.chipTextClosed,
};

const STATUS_LABEL: Record<Restaurant['status'], string> = {
  open: 'Open',
  paused: 'Paused',
  closed: 'Closed',
};

export default function RestaurantList({ restaurants }: RestaurantListProps) {
  const openCount = restaurants.filter((r) => r.status === 'open').length;

  return (
    <View style={styles.wrapper}>
      <Text style={styles.title}>Restaurants</Text>
      <Text style={styles.summary}>
        {openCount} of {restaurants.length} open
      </Text>

      {restaurants.map(({ id, name, status, latitude, longitude }) => (
        <View key={id} style={styles.card}>
          <View style={styles.cardTop}>
            <Text style={styles.name}>{name}</Text>

            <View style={[styles.chip, CHIP_STYLES[status]]}>
              <Text style={[styles.chipText, CHIP_TEXT_STYLES[status]]}>
                {STATUS_LABEL[status]}
              </Text>
            </View>
          </View>

          <Text style={styles.detail}>Restaurant ID: {id}</Text>

          {latitude !== null && longitude !== null && (
            <Text style={styles.detail}>
              Location: {latitude}, {longitude}
            </Text>
          )}
        </View>
      ))}
    </View>
  );
}