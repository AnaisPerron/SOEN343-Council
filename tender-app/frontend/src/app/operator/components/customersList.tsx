import { Text, View } from 'react-native';
import type { User } from '../../../types/users';
import { customerProfilesMock } from '../../../mocks/users.mock';
import { styles } from './customers.styles';
import { formatRegisteredSummary, getCustomerBalance } from '../operatorLogic';

interface CustomerListProps {
  customers?: User[]; 
}

export default function CustomerList({ customers = [] }: CustomerListProps) {
  return (
    <View style={styles.wrapper}>
      <Text style={styles.title}>Customers</Text>
      <Text style={styles.summary}>
        {formatRegisteredSummary(customers.length, 'Customer')}
      </Text>

      {customers.map(({ id, name, email, latitude, longitude }) => {
        const profile = customerProfilesMock.find((p) => p.user_id === id);
        const userLat = latitude ?? profile?.latitude;
        const userLng = longitude ?? profile?.longitude;

        return (
          <View key={id} style={styles.card}>
            <View style={styles.cardTop}>
              <Text style={styles.name}>{name}</Text>

              <View style={[styles.chip, styles.chipCustomer]}>
                <Text style={[styles.chipText, styles.chipTextCustomer]}>
                  Customer
                </Text>
              </View>
            </View>

            <Text style={styles.detail}>Email: {email}</Text>
            <Text style={styles.detail}>Customer ID: #{id}</Text>

            {userLat != null && userLng != null && (
              <Text style={styles.detail}>
                Location: {userLat}, {userLng}
              </Text>
            )}

            <View style={styles.cardBottom}>
              <Text style={styles.totalLabel}>TD Cash Balance:</Text>
              <Text style={styles.totalValue}>
                ${getCustomerBalance(id, customerProfilesMock)}
              </Text>
            </View>
          </View>
        );
      })}
    </View>
  );
}