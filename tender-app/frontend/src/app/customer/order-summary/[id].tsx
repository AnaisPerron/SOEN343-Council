//I, Anais Perron, certify that Codex helped me in generating the Template of this display. However, it has been heavily modified by myself
import { useEffect, useState } from 'react';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { fetchCustomerOrderSummary, type OrderSummary } from '@/api/orderSummary';
import { useAuth } from '@/context/AuthContext';

const money = (value: number) => `$${Number(value).toFixed(2)}`;

export default function CustomerOrderSummary() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { token } = useAuth();
  const [summary, setSummary] = useState<OrderSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id || !token) { setError('Order summary is unavailable.'); setLoading(false); return; }
    let active = true;
    setLoading(true);
    setError(null);
    fetchCustomerOrderSummary(id, token)
      .then((result) => { if (active) setSummary(result); })
      .catch((cause) => { if (active) setError(cause instanceof Error ? cause.message : 'Could not load order summary.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [id, token]);

  const subtotal = summary?.itemSubtotal ?? summary?.orderItems.reduce((sum, item) => sum + Number(item.lineTotal), 0) ?? 0;

  return <SafeAreaView style={styles.page}>
    <ScrollView contentContainerStyle={styles.content}>
      <Pressable onPress={() => router.replace('/customer/history')} accessibilityRole="button" style={styles.backButton}><Text style={styles.back}>‹ Back to orders</Text></Pressable>
      <Text style={styles.title}>Order summary</Text>
      {loading && <ActivityIndicator style={styles.loading} accessibilityLabel="Loading order summary" />}
      {error && <Text style={styles.error} accessibilityRole="alert">{error}</Text>}
      {summary && <>
        <Text style={styles.subtitle}>{summary.restaurantName ?? 'Restaurant'} · Order #{summary.orderId}</Text>
        <Text style={styles.subtitle}>Status: {summary.status?.replaceAll('_', ' ').toLowerCase() ?? 'Unknown'}</Text>
        <View style={styles.panel}>
          <Text style={styles.heading}>Items</Text>
          {summary.orderItems.map((item, index) => <View key={`${item.menuItemId}-${index}`} style={styles.row}>
            <Text style={styles.label}>{item.quantity} × {item.name}</Text>
            <Text style={styles.amount}>{money(item.lineTotal)}</Text>
          </View>)}
        </View>
        <View style={styles.panel}>
          <Text style={styles.heading}>Fare breakdown</Text>
          <View style={styles.row}><Text style={styles.label}>Items subtotal</Text><Text style={styles.amount}>{money(subtotal)}</Text></View>
          {summary.charges.map((charge, index) => <View key={`${charge.chargeType}-${index}`} style={styles.row}>
            <Text style={styles.label}>{charge.chargeType}</Text><Text style={styles.amount}>{money(charge.amount)}</Text>
          </View>)}
          <View style={[styles.row, styles.totalRow]}><Text style={styles.total}>Total</Text><Text style={styles.total}>{summary.totalCharge == null ? 'Unavailable' : money(summary.totalCharge)}</Text></View>
        </View>
        <View style={styles.panel}>
          <Text style={styles.heading}>Timestamps</Text>
          {summary.placedAt && <View style={styles.row}><Text style={styles.label}>Placed</Text><Text style={styles.amount}>{new Date(summary.placedAt).toLocaleString()}</Text></View>}
          {summary.acceptedAt && <View style={styles.row}><Text style={styles.label}>Accepted</Text><Text style={styles.amount}>{new Date(summary.acceptedAt).toLocaleString()}</Text></View>}
          {summary.pickedUpAt && <View style={styles.row}><Text style={styles.label}>Picked up</Text><Text style={styles.amount}>{new Date(summary.pickedUpAt).toLocaleString()}</Text></View>}
          {summary.deliveredAt && <View style={styles.row}><Text style={styles.label}>Delivered</Text><Text style={styles.amount}>{new Date(summary.deliveredAt).toLocaleString()}</Text></View>}
          {summary.cancelledAt && <View style={styles.row}><Text style={styles.label}>Cancelled</Text><Text style={styles.amount}>{new Date(summary.cancelledAt).toLocaleString()}</Text></View>}
          {summary.finishedAt && <View style={styles.row}><Text style={styles.label}>Finished</Text><Text style={styles.amount}>{new Date(summary.finishedAt).toLocaleString()}</Text></View>}
        </View>
      </>}
    </ScrollView>
  </SafeAreaView>;
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#F7F7F5' },
  content: { width: '100%', maxWidth: 800, alignSelf: 'center', padding: 22, paddingBottom: 80 },
  backButton: { alignSelf: 'flex-start', paddingVertical: 10 },
  back: { color: '#A92140', fontWeight: '700' },
  title: { color: '#242930', fontSize: 34, fontWeight: '800', marginTop: 12 },
  subtitle: { color: '#69717A', marginTop: 6, marginBottom: 22 },
  loading: { marginTop: 40 },
  error: { color: '#A83B4B', marginTop: 24 },
  panel: { backgroundColor: '#FFF', borderWidth: 1, borderColor: '#DDE2E3', borderRadius: 12, padding: 18, marginBottom: 16 },
  heading: { color: '#273039', fontSize: 18, fontWeight: '800', marginBottom: 12 },
  row: { flexDirection: 'row', justifyContent: 'space-between', gap: 16, paddingVertical: 8 },
  label: { color: '#38414A', flex: 1 },
  amount: { color: '#273039', textAlign: 'right', flexShrink: 1 },
  totalRow: { borderTopWidth: 1, borderTopColor: '#DDE2E3', marginTop: 8, paddingTop: 16 },
  total: { color: '#273039', fontSize: 18, fontWeight: '800' },
});
