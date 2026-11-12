import { Pressable, Text, TextStyle, View, ViewStyle } from 'react-native';
import type { OperatorOrder as Order } from '../../../types/operator';
import { styles } from './orders.styles';

interface OrderListProps {
  orders: Order[];
  disabled: boolean;
  onReassign?: (order: { id: number; courierId: number }) => void;
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
  refunded: styles.chipCancelled,
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
  refunded: styles.chipTextCancelled,
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
  refunded: 'Refunded',
};

export default function OrderList({ orders, disabled, onReassign }: OrderListProps) {
  const activeCount = orders.filter((o) => 
    ['placed', 'accepted', 'preparing', 'ready', 'picked_up'].includes(o.status)
  ).length;

  return (
    <View style={styles.wrapper}>
      <Text style={styles.title}>Orders</Text>
      <Text style={styles.summary}>
        {activeCount} active of {orders.length} loaded orders
      </Text>

      {orders.map(({ id, customerId, restaurantId, courierId, status, dateOrdered, rejectionReason, totalPrice }) => (
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
            Restaurant ID: {restaurantId ?? 'Unknown'} • Customer ID: {customerId ?? 'Unknown'}
          </Text>

          <Text style={styles.detail}>
            Courier ID: {courierId ?? 'Unassigned'}
          </Text>

          <Text style={styles.detail}>
            Placed: {dateOrdered && !Number.isNaN(Date.parse(dateOrdered)) ? new Date(dateOrdered).toLocaleString() : 'Unknown'}
          </Text>

          <View style={styles.cardBottom}>
            <Text style={styles.totalLabel}>Total Amount:</Text>
            <Text style={styles.totalValue}>${totalPrice.toFixed(2)}</Text>
          </View>

          {onReassign && status === 'ready' && courierId !== null && (
            <Pressable accessibilityRole="button" accessibilityLabel={`Reassign order ${id}`}
              disabled={disabled} accessibilityState={{ disabled }} style={[styles.reassign, disabled && styles.disabled]}
              onPress={() => onReassign({ id, courierId })}>
              <Text style={styles.reassignText}>Reassign courier</Text>
            </Pressable>
          )}

          {rejectionReason && (
            <Text style={styles.rejectionText}>Reason: {rejectionReason}</Text>
          )}
        </View>
      ))}
    </View>
  );
}