import { Text, View } from 'react-native';
import type { User } from '../../../types/users';
import { courierProfilesMock } from '../../../mocks/users.mock';
import { styles } from './customers.styles';
import { formatRegisteredSummary } from '../operatorLogic';

interface CourrierListProps {
  courrier?: User[]; 
}

export default function CouriersList({ courrier = [] }: CourrierListProps) {
  

  return (
    <View style={styles.wrapper}>
      <Text style={styles.title}>Couriers</Text>
      <Text style={styles.summary}>
        {formatRegisteredSummary(courrier.length, 'Courier')}
      </Text>

      {courrier.map(({ id, name, email, latitude, longitude }) => {
        const profile = courierProfilesMock.find((p) => p.user_id === id);
        const userLat = latitude ?? profile?.latitude;
        const userLng = longitude ?? profile?.longitude;

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

            {userLat != null && userLng != null && (
              <Text style={styles.detail}>
                Location: {userLat}, {userLng}
              </Text>
            )}

            
          </View>
        );
      })}
    </View>
  );
}