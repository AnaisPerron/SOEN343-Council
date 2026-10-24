import { Text, TextStyle, View, ViewStyle } from 'react-native';
import type { Order } from '../../../types/orders';
import { orderLinesMock } from '../../../mocks/orders.mock';
import { styles } from './orders.styles';

interface OrderListProps {
  orders: Order[];
}

const CHIP_STYLES: Record<Order['status'], ViewStyle> = {
  placed: styles.chipPlaced,
  accepted: styles.chipAccepted,
  preparing: styles.chipPreparing,
  ready: styles.chipReady,
  picked_up: styles.chipPickedUp,
  delivered: styles.chipDelivered,
  rejected: styles.chipRejected,
  cancelled: styles.chipCancelled,
};

const CHIP_TEXT_STYLES: Record<Order['status'], TextStyle> = {
  placed: styles.chipTextPlaced,
  accepted: styles.chipTextAccepted,
  preparing: styles.chipTextPreparing,
  ready: styles.chipTextReady,
  picked_up: styles.chipTextPickedUp,
  delivered: styles.chipTextDelivered,
  rejected: styles.chipTextRejected,
  cancelled: styles.chipTextCancelled,
};

const STATUS_LABEL: Record<Order['status'], string> = {
  placed: 'Placed',
  accepted: 'Accepted',
  preparing: 'Preparing',
  ready: 'Ready',
  picked_up: 'Picked Up',
  delivered: 'Delivered',
  rejected: 'Rejected',
  cancelled: 'Cancelled',
};

export default function OrderList({ orders }: OrderListProps) {
  const calculateOrderTotal = (orderId: number) => {
    const lines = orderLinesMock.filter((line) => line.order_id === orderId);
    const total = lines.reduce((sum, item) => sum + item.quantity * item.unit_price, 0);
    return total.toFixed(2);
  };

  const activeCount = orders.filter((o) => 
    ['placed', 'accepted', 'preparing', 'ready', 'picked_up'].includes(o.status)
  ).length;

  return (
    <View style={styles.wrapper}>
      <Text style={styles.title}>Orders</Text>
      <Text style={styles.summary}>
        {activeCount} active of {orders.length} total orders
      </Text>

      {orders.map(({ id, customer_id, restaurant_id, courier_id, status, created_at, rejection_reason }) => (
        <View key={id} style={styles.card}>
          <View style={styles.cardTop}>
            <Text style={styles.name}>Order #{id}</Text>

            <View style={[styles.chip, CHIP_STYLES[status] ?? {}]}>
              <Text style={[styles.chipText, CHIP_TEXT_STYLES[status] ?? {}]}>
                {STATUS_LABEL[status]}
              </Text>
            </View>
          </View>

          <Text style={styles.detail}>
            Restaurant ID: {restaurant_id} • Customer ID: {customer_id}
          </Text>

          <Text style={styles.detail}>
            Courier ID: {courier_id ?? 'Unassigned'}
          </Text>

          <Text style={styles.detail}>
            Placed: {new Date(created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </Text>

          <View style={styles.cardBottom}>
            <Text style={styles.totalLabel}>Total Amount:</Text>
            <Text style={styles.totalValue}>${calculateOrderTotal(id)}</Text>
          </View>

          {rejection_reason && (
            <Text style={styles.rejectionText}>Reason: {rejection_reason}</Text>
          )}
        </View>
      ))}
    </View>
  );
}