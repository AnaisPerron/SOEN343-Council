import { Text, View } from 'react-native';
import type { OperatorCustomer } from '../../../types/operator';
import { styles } from './customers.styles';

interface CustomerListProps {
  customers?: OperatorCustomer[];
}

export default function CustomerList({ customers = [] }: CustomerListProps) {
  return (
    <View style={styles.wrapper}>
      <Text style={styles.title}>Customers</Text>
      <Text style={styles.summary}>
        {customers.length} Loaded {customers.length === 1 ? 'Customer' : 'Customers'}
      </Text>

      {customers.map(({ id, name, email, latitude, longitude, tdCashBalance }) => {
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

            {latitude != null && longitude != null && (
              <Text style={styles.detail}>
                Location: {latitude}, {longitude}
              </Text>
            )}

            <View style={styles.cardBottom}>
              <Text style={styles.totalLabel}>TD Cash Balance:</Text>
              <Text style={styles.totalValue}>
                {tdCashBalance === null ? 'Unknown' : `$${tdCashBalance.toFixed(2)}`}
              </Text>
            </View>
          </View>
        );
      })}
    </View>
  );
}
