import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import Animated, {
  FadeIn,
  FadeInDown,
  LinearTransition,
} from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useLocalSearchParams } from 'expo-router';
import { useAuth } from '@/context/AuthContext';
import {
  fetchOrders,
  observeOrderStatus,
  transitionOrder,
  type IncomingOrder,
  type OrderAction,
} from '@/lib/orderApi';

function formatOrderTime(timestamp: string | null, label: string): string {
  if (!timestamp) {
    return `${label} time unavailable`;
  }

  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) {
    return `${label} time unavailable`;
  }

  return `${label} ${date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}`;
}

export default function IncomingOrdersScreen() {
  const { user } = useAuth();
  const params = useLocalSearchParams<{ restaurantId?: string | string[] }>();
  const routeRestaurantId = Array.isArray(params.restaurantId)
    ? params.restaurantId[0]
    : params.restaurantId;
  const restaurantId = user?.role === 'restaurant' && user.restaurantId
    ? String(user.restaurantId)
    : routeRestaurantId?.trim() || process.env.EXPO_PUBLIC_RESTAURANT_ID?.trim() || '1';
  const [orders, setOrders] = useState<IncomingOrder[]>([]);
  const [queue, setQueue] = useState<'incoming' | 'preparation' | 'pickup'>('incoming');
  const [rejectingOrderId, setRejectingOrderId] = useState<number | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [actingOrderId, setActingOrderId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const loadOrders = useCallback(async (id: string, selectedQueue = queue) => {
    const selectedRestaurantId = id.trim();
    if (!selectedRestaurantId) {
      setOrders([]);
      setError('Enter a restaurant ID to view incoming orders.');
      return;
    }

    setLoading(true);
    setError(null);
    setOrders([]);
    try {
      setOrders(await fetchOrders(selectedRestaurantId, selectedQueue));
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Unable to load incoming orders.');
    } finally {
      setLoading(false);
    }
  }, [queue]);

  useEffect(() => {
    let cancelled = false;
    fetchOrders(restaurantId, queue)
      .then((loadedOrders) => {
        if (!cancelled) {
          setOrders(loadedOrders);
          setError(null);
        }
      })
      .catch((loadError: unknown) => {
        if (!cancelled) {
          setOrders([]);
          setError(loadError instanceof Error ? loadError.message : 'Unable to load incoming orders.');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [restaurantId, queue]);

  useEffect(() => {
    if (Platform.OS !== 'web') {
      return;
    }

    const stopObserving = observeOrderStatus(restaurantId, () => {
      void loadOrders(restaurantId, queue);
    });
    return stopObserving;
  }, [restaurantId, queue, loadOrders]);

  const performAction = async (order: IncomingOrder, action: OrderAction, reason?: string) => {
    setActingOrderId(order.id);
    setError(null);
    setActionMessage(null);
    try {
      await transitionOrder(restaurantId.trim(), order.id, action, reason);
      const successMessages: Record<OrderAction, string> = {
        accept: `Order #${order.id} has been accepted.`,
        reject: `Order #${order.id} has been rejected.`,
        cancel: `Order #${order.id} has been cancelled.`,
        prepare: `Order #${order.id} is now preparing.`,
        ready: `Order #${order.id} has been marked ready.`,
      };
      setActionMessage(successMessages[action]);
      setRejectingOrderId(null);
      setRejectionReason('');
      if (action === 'accept') {
        setLoading(true);
        setQueue('preparation');
      } else {
        await loadOrders(restaurantId, queue);
      }
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : 'Unable to update order.');
    } finally {
      setActingOrderId(null);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.eyebrow}>RESTAURANT DASHBOARD</Text>
        <Text style={styles.title}>Restaurant orders</Text>
        <Text style={styles.subtitle}>
          Review new orders and move them through preparation.
        </Text>

        <View style={styles.profileCard}>
          <View>
            <Text style={styles.profileLabel}>ACTIVE RESTAURANT</Text>
            <Text style={styles.profileName}>Restaurant #{restaurantId}</Text>
          </View>
          <Pressable
            accessibilityRole="button"
            disabled={loading}
            onPress={() => void loadOrders(restaurantId, queue)}
            style={({ pressed }) => [
              styles.refreshButton,
              (pressed || loading) && styles.buttonDimmed,
            ]}
          >
            <Text style={styles.refreshButtonText}>{loading ? 'Loading…' : 'Refresh'}</Text>
          </Pressable>
        </View>

        <View style={styles.queueTabs}>
          {(['incoming', 'preparation', 'pickup'] as const).map((tab) => (
            <Pressable
              key={tab}
              accessibilityRole="button"
              onPress={() => {
                setLoading(true);
                setError(null);
                setQueue(tab);
              }}
              style={[styles.queueTab, queue === tab && styles.queueTabSelected]}
            >
              <Text style={[styles.queueTabText, queue === tab && styles.queueTabTextSelected]}>
                {tab === 'incoming'
                  ? 'Arriving'
                  : tab === 'preparation'
                    ? 'Preparing'
                    : 'Awaiting pickup'}
              </Text>
            </Pressable>
          ))}
        </View>

        {actionMessage && (
          <Animated.View entering={FadeIn.duration(220)} accessibilityRole="alert" style={styles.successCard}>
            <Text style={styles.successText}>{actionMessage}</Text>
          </Animated.View>
        )}

        {loading ? (
          <Animated.View entering={FadeIn.duration(180)} style={styles.stateCard}>
            <ActivityIndicator color="#E94057" size="large" />
            <Text style={styles.stateText}>
              Loading {queue === 'incoming' ? 'incoming' : queue === 'preparation' ? 'preparation' : 'pickup'} orders…
            </Text>
          </Animated.View>
        ) : error ? (
          <Animated.View entering={FadeIn.duration(180)} accessibilityRole="alert" style={styles.errorCard}>
            <Text style={styles.errorTitle}>Couldn’t load orders</Text>
            <Text style={styles.errorText}>{error}</Text>
            <Pressable
              accessibilityRole="button"
              onPress={() => void loadOrders(restaurantId, queue)}
              style={styles.retryButton}
            >
              <Text style={styles.retryButtonText}>Try again</Text>
            </Pressable>
          </Animated.View>
        ) : restaurantId.trim() && orders.length === 0 ? (
          <Animated.View entering={FadeIn.duration(180)} style={styles.stateCard}>
            <Text style={styles.emptyTitle}>You’re all caught up</Text>
            <Text style={styles.stateText}>
              There are no {queue === 'incoming' ? 'incoming' : queue === 'preparation' ? 'preparation' : 'pickup'} orders.
            </Text>
          </Animated.View>
        ) : (
          orders.map((order, index) => (
            <Animated.View
              key={order.id}
              entering={FadeInDown.duration(260).delay(index * 45)}
              layout={LinearTransition.springify().damping(18)}
              style={styles.orderCard}
            >
              <View style={styles.orderHeader}>
                <View>
                  <Text style={styles.orderId}>Order #{order.id}</Text>
                  <Text style={styles.arrivalTime}>
                    {queue === 'incoming'
                      ? formatOrderTime(order.dateOrdered, 'Received')
                      : formatOrderTime(order.acceptedAt, 'Accepted')}
                  </Text>
                </View>
                <Text style={styles.status}>
                  {order.status === 'PREPARING' ? 'Preparing...' : order.status}
                </Text>
              </View>
              <View style={styles.divider} />
              {order.orderLines.length > 0 ? (
                order.orderLines.map((item, index) => (
                  <View key={`${order.id}-${item.itemName}-${index}`} style={styles.itemRow}>
                    <Text style={styles.itemName}>{item.itemName}</Text>
                    <Text style={styles.quantity}>× {item.quantity}</Text>
                    <Text style={styles.quantity}>${item.unitPrice.toFixed(2)} each</Text>
                  </View>
                ))
              ) : (
                <Text style={styles.stateText}>No order lines listed.</Text>
              )}
              <Text style={styles.total}>Total ${order.totalPrice?.toFixed(2) ?? '0.00'}</Text>
              {order.rejectionReason && (
                <Text style={styles.rejectionReason}>Rejection reason: {order.rejectionReason}</Text>
              )}
              <View style={styles.actions}>
                {order.status === 'PLACED' && (
                  <>
                    <ActionButton
                      label="Accept"
                      disabled={actingOrderId === order.id}
                      onPress={() => void performAction(order, 'accept')}
                    />
                    <ActionButton
                      label="Reject"
                      disabled={actingOrderId === order.id}
                      onPress={() => {
                        if (rejectingOrderId === order.id) {
                          setRejectingOrderId(null);
                          setRejectionReason('');
                          return;
                        }
                        setRejectingOrderId(order.id);
                        setRejectionReason('');
                      }}
                    />
                    <ActionButton
                      label="Cancel"
                      disabled={actingOrderId === order.id}
                      onPress={() => void performAction(order, 'cancel')}
                    />
                  </>
                )}
                {order.status === 'ACCEPTED' && (
                  <ActionButton
                    label="Start preparing"
                    disabled={actingOrderId === order.id}
                    onPress={() => void performAction(order, 'prepare')}
                  />
                )}
                {order.status === 'PREPARING' && (
                  <ActionButton
                    label="Mark ready"
                    disabled={actingOrderId === order.id}
                    onPress={() => void performAction(order, 'ready')}
                  />
                )}
              </View>
              {rejectingOrderId === order.id && (
                <View style={styles.rejectForm}>
                  <TextInput
                    accessibilityLabel={`Rejection reason for order ${order.id}`}
                    onChangeText={setRejectionReason}
                    placeholder="Required rejection reason"
                    style={styles.input}
                    value={rejectionReason}
                  />
                  <ActionButton
                    label={actingOrderId === order.id ? 'Submitting…' : 'Reject order'}
                    disabled={actingOrderId === order.id || !rejectionReason.trim()}
                    onPress={() => void performAction(order, 'reject', rejectionReason)}
                  />
                </View>
              )}
            </Animated.View>
          ))
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  container: {
    width: '100%',
    maxWidth: 760,
    alignSelf: 'center',
    padding: 24,
    paddingBottom: 48,
  },
  eyebrow: {
    color: '#E94057',
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 1.4,
  },
  title: {
    color: '#2D2527',
    fontSize: 32,
    fontWeight: '700',
    marginTop: 8,
  },
  subtitle: {
    color: '#74696B',
    fontSize: 16,
    lineHeight: 23,
    marginTop: 8,
  },
  profileCard: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    marginTop: 24,
    marginBottom: 20,
    padding: 16,
    borderWidth: 1,
    borderColor: '#F1E5E7',
    borderRadius: 14,
    backgroundColor: '#FFFFFF',
  },
  profileLabel: {
    color: '#85797B',
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1,
  },
  profileName: {
    marginTop: 5,
    color: '#2D2527',
    fontSize: 16,
    fontWeight: '700',
  },
  queueTabs: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 20,
  },
  queueTab: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 10,
    backgroundColor: '#F8F2F3',
  },
  queueTabSelected: {
    backgroundColor: '#E94057',
  },
  queueTabText: {
    color: '#43383A',
    fontWeight: '600',
  },
  queueTabTextSelected: {
    color: '#FFFFFF',
  },
  successCard: {
    marginBottom: 16,
    padding: 14,
    borderWidth: 1,
    borderColor: '#F4CBD1',
    borderRadius: 12,
    backgroundColor: '#FFF1F3',
  },
  successText: {
    color: '#9B2638',
    fontSize: 15,
    fontWeight: '600',
  },
  input: {
    flex: 1,
    minWidth: 0,
    minHeight: 48,
    paddingHorizontal: 14,
    borderWidth: 1,
    borderColor: '#E7DADD',
    borderRadius: 10,
    color: '#2D2527',
    backgroundColor: '#FFFFFF',
    fontSize: 16,
  },
  refreshButton: {
    minHeight: 48,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 16,
    borderRadius: 10,
    backgroundColor: '#E94057',
  },
  refreshButtonText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '700',
  },
  buttonDimmed: {
    opacity: 0.7,
  },
  stateCard: {
    minHeight: 150,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
    padding: 24,
    borderRadius: 14,
    backgroundColor: '#FFFFFF',
  },
  stateText: {
    color: '#85797B',
    fontSize: 15,
    lineHeight: 22,
    textAlign: 'center',
  },
  emptyTitle: {
    color: '#43383A',
    fontSize: 18,
    fontWeight: '700',
  },
  errorCard: {
    gap: 8,
    padding: 20,
    borderWidth: 1,
    borderColor: '#F4CBD1',
    borderRadius: 14,
    backgroundColor: '#FFF4F5',
  },
  errorTitle: {
    color: '#A52D3D',
    fontSize: 17,
    fontWeight: '700',
  },
  errorText: {
    color: '#7C3B45',
    fontSize: 14,
    lineHeight: 21,
  },
  retryButton: {
    alignSelf: 'flex-start',
    marginTop: 6,
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 8,
    backgroundColor: '#E94057',
  },
  retryButtonText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '600',
  },
  orderCard: {
    padding: 18,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: '#F1E5E7',
    borderRadius: 14,
    backgroundColor: '#FFFFFF',
  },
  orderHeader: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 12,
  },
  orderId: {
    color: '#2D2527',
    fontSize: 18,
    fontWeight: '700',
  },
  arrivalTime: {
    color: '#85797B',
    fontSize: 13,
    marginTop: 4,
  },
  status: {
    overflow: 'hidden',
    paddingVertical: 5,
    paddingHorizontal: 9,
    borderRadius: 20,
    color: '#A52D3D',
    backgroundColor: '#FFF1F3',
    fontSize: 12,
    fontWeight: '700',
  },
  divider: {
    height: 1,
    marginVertical: 14,
    backgroundColor: '#F3E9EB',
  },
  itemRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
    marginBottom: 10,
  },
  itemName: {
    flex: 1,
    color: '#43383A',
    fontSize: 15,
  },
  quantity: {
    color: '#43383A',
    fontSize: 15,
    fontWeight: '600',
  },
  total: {
    alignSelf: 'flex-end',
    marginTop: 8,
    color: '#2D2527',
    fontSize: 15,
    fontWeight: '700',
  },
  actions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: 14,
  },
  actionButton: {
    minHeight: 40,
    justifyContent: 'center',
    paddingHorizontal: 14,
    borderRadius: 9,
    backgroundColor: '#E94057',
  },
  actionButtonText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '700',
  },
  actionButtonPressed: {
    transform: [{ scale: 0.97 }],
  },
  rejectForm: {
    gap: 8,
    marginTop: 12,
  },
  rejectionReason: {
    marginTop: 8,
    color: '#8B3D32',
  },
});

function ActionButton({
  label,
  disabled,
  onPress,
}: {
  label: string;
  disabled: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.actionButton,
        pressed && styles.actionButtonPressed,
        disabled && styles.buttonDimmed,
      ]}
    >
      <Text style={styles.actionButtonText}>{label}</Text>
    </Pressable>
  );
}
