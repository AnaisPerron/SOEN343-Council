import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  Platform,
  ScrollView,
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
import { Redirect, useRouter } from 'expo-router';
import { useAuth } from '@/context/AuthContext';
import AccountMenu from '@/components/AccountMenu';
import {
  fetchOrders,
  observeOrderStatus,
  transitionOrder,
  type IncomingOrder,
  type OrderAction,
} from '@/lib/orderApi';
import { styles } from './incoming-orders.styles';

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
  const { user, ready } = useAuth();
  if (!ready) return <SafeAreaView style={styles.safeArea}><ActivityIndicator color="#E94057" /></SafeAreaView>;
  if (!user) return <Redirect href="/auth/login" />;
  if (user.role !== 'restaurant') return <Redirect href="/account" />;
  if (!user.restaurantId) return <SafeAreaView style={styles.safeArea}><Text style={styles.missingRestaurant}>This account is not linked to a restaurant.</Text></SafeAreaView>;
  return <RestaurantOrdersContent restaurantId={String(user.restaurantId)} />;
}

function RestaurantOrdersContent({ restaurantId }: { restaurantId: string }) {
  const router = useRouter();
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
        <Pressable accessibilityRole="button" onPress={() => router.replace('/restaurant')}><Text style={styles.dashboardBack}>‹  Restaurant dashboard</Text></Pressable>
        <View style={styles.screenHeader}><Text style={styles.eyebrow}>RESTAURANT DASHBOARD</Text><AccountMenu /></View>
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
