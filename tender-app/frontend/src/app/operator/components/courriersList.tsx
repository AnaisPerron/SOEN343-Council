import { Text, View } from 'react-native';
import type { OperatorCourier } from '../../../types/operator';
import { styles } from './customers.styles';

interface CourrierListProps {
  couriers?: OperatorCourier[];
}

export default function CouriersList({ couriers = [] }: CourrierListProps) {


  return (
    <View style={styles.wrapper}>
      <Text style={styles.title}>Couriers</Text>
      <Text style={styles.summary}>
        {couriers.length} Loaded {couriers.length === 1 ? 'Courier' : 'Couriers'}
      </Text>

      {couriers.map(({ id, name, email, latitude, longitude, status }) => {
        return (
          <View key={id} style={styles.card}>
            <View style={styles.cardTop}>
              <Text style={styles.name}>{name}</Text>

              <View style={[styles.chip, styles.chipCustomer]}>
                <Text style={[styles.chipText, styles.chipTextCustomer]}>
                  Courier
                </Text>
              </View>
            </View>

            <Text style={styles.detail}>Email: {email}</Text>
            <Text style={styles.detail}>Courier ID: #{id}</Text>
            <Text style={styles.detail}>Status: {status ?? 'Unknown'}</Text>

            {latitude != null && longitude != null && (
              <Text style={styles.detail}>
                Location: {latitude}, {longitude}
              </Text>
            )}


          </View>
        );
      })}
    </View>
  );
}
