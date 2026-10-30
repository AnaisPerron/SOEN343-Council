import { Text, TextStyle, TouchableOpacity, View, ViewStyle } from 'react-native';
import type { OperatorRestaurant as Restaurant, RestaurantStatus } from '../../../types/operator';
import { styles } from './restaurants.styles';

interface RestaurantListProps {
  restaurants: Restaurant[];
  changingId: number | null;
  disabled: boolean;
  onStatusChange: (id: number, status: RestaurantStatus) => Promise<void>;
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

export default function RestaurantList({ restaurants, changingId, disabled, onStatusChange }: RestaurantListProps) {
  const openCount = restaurants.filter((r) => r.status === 'open').length;

  return (
    <View style={styles.wrapper}>
      <Text style={styles.title}>Restaurants</Text>
      <Text style={styles.summary}>
        {openCount} of {restaurants.length} loaded restaurants open
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
          <TouchableOpacity style={styles.action} disabled={disabled} accessibilityRole="button"
            accessibilityLabel={`${status === 'open' ? 'Pause' : 'Reopen'} ${name}`}
            onPress={() => void onStatusChange(id, status === 'open' ? 'paused' : 'open')}>
            <Text style={styles.actionText}>{changingId === id ? 'Saving…' : status === 'open' ? 'Pause' : 'Reopen'}</Text>
          </TouchableOpacity>

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