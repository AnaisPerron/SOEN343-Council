import { Text, TextStyle, View, ViewStyle,TouchableOpacity } from 'react-native';
import type { OperatorOrder as Order } from '../../../types/operator';
import { styles } from './orders.styles';
import { useState } from 'react';
import { canRefundOrder,canReassignOrder } from '../operatorLogic';


interface OrderListProps {
  orders: Order[];
  onRefund: (orderId: number) => void;
  onReassign: (orderId: number) => void;
  changingId: number | null;
  disabled?: boolean;
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

export default function OrderList({ orders,onRefund,onReassign,changingId,disabled }: OrderListProps) {
  const activeCount = orders.filter((o) => 
    ['placed', 'accepted', 'preparing', 'ready', 'picked_up'].includes(o.status)
  ).length;
  const [confirmRefundId, setConfirmRefundId] = useState<number | null>(null);
  const locked = !!disabled || changingId != null;

  return (
    <View style={styles.wrapper}>
      <Text style={styles.title}>Orders</Text>
      <Text style={styles.summary}>
        {activeCount} active of {orders.length} loaded orders
      </Text>

      {orders.map(({ id, customerId, restaurantId, courierId, courierOnline, status, dateOrdered, rejectionReason, totalPrice }) => {
        const showRefund = canRefundOrder({ status });
        const showReassign = canReassignOrder({ status, courierId, courierOnline });
        const isChanging = changingId === id;
        const confirming = confirmRefundId === id;
      
      return(
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
            {courierId != null && courierOnline === false ? ' (offline)' : ''}
          </Text>

          <Text style={styles.detail}>
            Placed: {dateOrdered && !Number.isNaN(Date.parse(dateOrdered)) ? new Date(dateOrdered).toLocaleString() : 'Unknown'}
          </Text>

          <View style={styles.cardBottom}>
            <Text style={styles.totalLabel}>Total Amount:</Text>
            <Text style={styles.totalValue}>${totalPrice.toFixed(2)}</Text>
          </View>

          {rejectionReason && (
            <Text style={styles.rejectionText}>Reason: {rejectionReason}</Text>
          )}
           {(showRefund || showReassign) && (
              <View style={styles.actionsRow}>
                {showRefund && (
                  <TouchableOpacity
                    onPress={() => {
                      if (!confirming) { setConfirmRefundId(id); return; }
                      setConfirmRefundId(null);
                      onRefund(id);
                    }}
                    disabled={locked}
                    accessibilityRole="button"
                    accessibilityState={{ disabled: locked }}
                    style={[styles.actionButton, styles.actionButtonDanger, locked && styles.actionButtonDisabled]}
                  >
                    <Text style={[styles.actionButtonText, styles.actionButtonTextDanger]}>
                      {isChanging ? 'Updating…' : confirming ? 'Confirm refund?' : 'Refund'}
                    </Text>
                  </TouchableOpacity>
                )}
                {showReassign && (
                  <TouchableOpacity
                    onPress={() => onReassign(id)}
                    disabled={locked}
                    accessibilityRole="button"
                    accessibilityState={{ disabled: locked }}
                    style={[styles.actionButton, locked && styles.actionButtonDisabled]}
                  >
                    <Text style={styles.actionButtonText}>
                      {isChanging ? 'Updating…' : 'Reassign courier'}
                    </Text>
                  </TouchableOpacity>
                )}

                {confirming && (
                  <TouchableOpacity
                    onPress={() => setConfirmRefundId(null)}
                    accessibilityRole="button"
                    style={styles.actionButton}
                  >
                    <Text style={styles.actionButtonText}>Back</Text>
                  </TouchableOpacity>
                )}
              </View>
            )}
          </View>
        );
      })}
    </View>
  );
}