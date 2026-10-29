import { useEffect, useMemo, useState } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';

import { useAuth } from '@/context/AuthContext';
import { demoRestaurantForToken } from '@/mocks/restaurantAccount';
import { loadRestaurantOrders, saveRestaurantOrders } from '@/storage/restaurantOrders';
import {
  deleteSelectedOrders, filterAndSortOrders, ORDER_STATUSES, orderTotal, transitionOrder,
  type OrderFilter, type RestaurantOrder, type RestaurantOrderStatus,
} from '@/services/restaurant/orderQueue';

const label = (status: RestaurantOrderStatus) => status.replace('_', ' ').replace(/^./, (letter) => letter.toUpperCase());
const dateTime = (value: string) => new Date(value).toLocaleString('en-CA', { dateStyle: 'medium', timeStyle: 'short' });
const money = (value: number) => `$${value.toFixed(2)}`;

export default function RestaurantOrders() {
  const router = useRouter();
  const { user, token } = useAuth();
  const demo = token ? demoRestaurantForToken(token) : null;
  const [orders, setOrders] = useState<RestaurantOrder[]>(() => demo ? loadRestaurantOrders(demo.restaurantId) : []);
  const [filter, setFilter] = useState<OrderFilter>('placed');
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [detailId, setDetailId] = useState<number | null>(null);
  const [rejecting, setRejecting] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!token || user?.role !== 'restaurant') router.replace('/auth/login');
  }, [token, user?.role, router]);

  const shown = useMemo(() => filterAndSortOrders(orders, filter), [orders, filter]);
  const detail = orders.find((order) => order.id === detailId) ?? null;
  const allShownSelected = shown.length > 0 && shown.every((order) => selected.has(order.id));

  const persist = (next: RestaurantOrder[]) => {
    if (!demo) return;
    setOrders(next);
    try { saveRestaurantOrders(demo.restaurantId, next); setMessage(''); }
    catch { setMessage('Changes are kept for this session, but browser storage is full.'); }
  };

  const toggleSelected = (id: number) => setSelected((current) => {
    const next = new Set(current);
    if (next.has(id)) next.delete(id); else next.add(id);
    return next;
  });

  const changeStatus = (nextStatus: RestaurantOrderStatus, reason?: string) => {
    if (!detail) return;
    try {
      const updated = transitionOrder(detail, nextStatus, new Date().toISOString(), reason);
      persist(orders.map((order) => order.id === detail.id ? updated : order));
      setRejecting(false);
      setRejectionReason('');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not update the order.');
    }
  };

  const removeSelected = () => {
    persist(deleteSelectedOrders(orders, selected));
    if (detailId !== null && selected.has(detailId)) setDetailId(null);
    setSelected(new Set());
    setConfirmDelete(false);
  };

  if (!user || user.role !== 'restaurant') return null;

  return <SafeAreaView style={styles.page}>
    <ScrollView contentContainerStyle={styles.content}>
      <Pressable accessibilityRole="button" onPress={() => router.back()}><Text style={styles.back}>‹  Restaurant dashboard</Text></Pressable>
      <Text style={styles.eyebrow}>RESTAURANT OPERATIONS</Text>
      <Text style={styles.title}>Orders queue</Text>
      <Text style={styles.subtitle}>Review incoming orders and move each order through preparation.</Text>

      {!demo ? <View style={styles.empty}>
        <Text style={styles.emptyTitle}>No order feed connected</Text>
        <Text style={styles.emptyText}>The queue is available for offline demo sessions. Backend orders are not connected in this frontend.</Text>
      </View> : <>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filters} contentContainerStyle={styles.filterContent}>
          {(['all', ...ORDER_STATUSES] as OrderFilter[]).map((status) => {
            const count = status === 'all' ? orders.length : orders.filter((order) => order.status === status).length;
            return <Pressable key={status} accessibilityRole="button" accessibilityState={{ selected: filter === status }} style={[styles.filter, filter === status && styles.filterActive]} onPress={() => setFilter(status)}>
              <Text style={[styles.filterText, filter === status && styles.filterTextActive]}>{status === 'all' ? 'All' : label(status)}  {count}</Text>
            </Pressable>;
          })}
        </ScrollView>

        <View style={styles.queueToolbar}>
          <Text style={styles.queueCount}>{shown.length} {shown.length === 1 ? 'order' : 'orders'} · oldest first</Text>
          {shown.length > 0 && <Pressable accessibilityRole="button" onPress={() => setSelected((current) => {
            const next = new Set(current);
            for (const order of shown) { if (allShownSelected) next.delete(order.id); else next.add(order.id); }
            return next;
          })}>
            <Text style={styles.toolbarLink}>{allShownSelected ? 'Clear shown selection' : 'Select all shown'}</Text>
          </Pressable>}
        </View>
        {selected.size > 0 && <Pressable accessibilityRole="button" style={styles.deleteButton} onPress={() => setConfirmDelete(true)}><Text style={styles.deleteButtonText}>Delete selected ({selected.size})</Text></Pressable>}
        {!!message && <Text style={styles.message}>{message}</Text>}

        {shown.length === 0 ? <View style={styles.empty}>
          <Text style={styles.emptyTitle}>No orders in this status</Text>
          <Text style={styles.emptyText}>Choose another status to view the rest of the queue.</Text>
        </View> : shown.map((order) => <View key={order.id} style={styles.orderRow}>
          <Pressable accessibilityRole="checkbox" accessibilityState={{ checked: selected.has(order.id) }} accessibilityLabel={`Select order ${order.id} for deletion`} onPress={() => toggleSelected(order.id)} style={styles.checkArea}>
            <View style={[styles.checkbox, selected.has(order.id) && styles.checkboxChecked]}>{selected.has(order.id) && <Text style={styles.checkmark}>✓</Text>}</View>
          </Pressable>
          <Pressable accessibilityRole="button" accessibilityLabel={`View order ${order.id} details`} style={styles.orderBody} onPress={() => setDetailId(order.id)}>
            <View style={styles.orderLines}>{order.lines.map((line, index) => <View key={`${order.id}-${index}`} style={index > 0 ? styles.additionalLine : undefined}>
              <Text style={styles.itemName}>{line.quantity} × {line.name}</Text>
              {line.toppings.length > 0 && <Text style={styles.toppings}>{line.toppings.map((topping) => topping.name).join(' · ')}</Text>}
            </View>)}</View>
            <View style={styles.rowEnd}><StatusBadge status={order.status} /><Text style={styles.openHint}>View details  ›</Text></View>
          </Pressable>
        </View>)}
      </>}
    </ScrollView>

    <Modal transparent visible={detail !== null && !rejecting} animationType="fade" onRequestClose={() => setDetailId(null)}>
      <View style={styles.modalBackdrop}><View style={styles.modalCard}>
        {detail && <ScrollView contentContainerStyle={styles.modalContent}>
          <View style={styles.modalHeading}><View><Text style={styles.modalEyebrow}>ORDER RECORD</Text><Text style={styles.modalTitle}>Order #{detail.id}</Text></View><Pressable accessibilityRole="button" accessibilityLabel="Close order details" onPress={() => setDetailId(null)}><Text style={styles.close}>×</Text></Pressable></View>
          <StatusBadge status={detail.status} />
          <View style={styles.divider} />
          <Info label="Placed" value={dateTime(detail.createdAt)} />
          {detail.acceptedAt && <Info label="Accepted" value={dateTime(detail.acceptedAt)} />}
          <Info label="Last updated" value={dateTime(detail.updatedAt)} />
          <Info label="Customer" value={detail.customer.name} />
          <Info label="Delivery address" value={detail.customer.address} />
          <Info label="Contact" value={detail.customer.phone} />
          {detail.rejectionReason && <Info label="Rejection reason" value={detail.rejectionReason} />}
          <View style={styles.divider} />
          <Text style={styles.sectionTitle}>Items</Text>
          {detail.lines.map((line, index) => <View key={`${detail.id}-detail-${index}`} style={styles.detailLine}>
            <View style={styles.detailLineHeader}><Text style={styles.detailItemName}>{line.quantity} × {line.name}</Text><Text style={styles.detailPrice}>{money(line.quantity * (line.unitPrice + line.toppings.reduce((total, topping) => total + topping.additionalCost, 0)))}</Text></View>
            {line.toppings.map((topping, toppingIndex) => <Text key={toppingIndex} style={styles.detailTopping}>+ {topping.name} · {money(topping.additionalCost)} each</Text>)}
          </View>)}
          <View style={styles.totalRow}><Text style={styles.totalLabel}>Order total</Text><Text style={styles.totalValue}>{money(orderTotal(detail))}</Text></View>
          {detail.status === 'placed' && <View style={styles.actions}>
            <Pressable accessibilityRole="button" style={styles.primaryAction} onPress={() => changeStatus('accepted')}><Text style={styles.primaryActionText}>Accept order</Text></Pressable>
            <Pressable accessibilityRole="button" style={styles.secondaryAction} onPress={() => setRejecting(true)}><Text style={styles.secondaryActionText}>Reject order</Text></Pressable>
          </View>}
          {detail.status === 'accepted' && <Pressable accessibilityRole="button" style={styles.primaryAction} onPress={() => changeStatus('preparing')}><Text style={styles.primaryActionText}>Start preparing</Text></Pressable>}
          {detail.status === 'preparing' && <Pressable accessibilityRole="button" style={styles.primaryAction} onPress={() => changeStatus('ready')}><Text style={styles.primaryActionText}>Mark ready for pickup</Text></Pressable>}
        </ScrollView>}
      </View></View>
    </Modal>

    <Modal transparent visible={rejecting} animationType="fade" onRequestClose={() => setRejecting(false)}>
      <View style={styles.modalBackdrop}><View style={styles.dialog}>
        <Text style={styles.modalEyebrow}>ORDER DECISION</Text><Text style={styles.modalTitle}>Reject order</Text>
        <Text style={styles.dialogText}>Give the customer a clear reason for the rejection.</Text>
        <TextInput accessibilityLabel="Rejection reason" multiline value={rejectionReason} onChangeText={setRejectionReason} placeholder="e.g. Selected ingredient is unavailable" style={styles.reasonInput} />
        <View style={styles.dialogActions}><Pressable accessibilityRole="button" onPress={() => { setRejecting(false); setRejectionReason(''); }}><Text style={styles.toolbarLink}>Cancel</Text></Pressable><Pressable accessibilityRole="button" disabled={!rejectionReason.trim()} style={[styles.dangerAction, !rejectionReason.trim() && styles.disabledAction]} onPress={() => changeStatus('rejected', rejectionReason)}><Text style={styles.primaryActionText}>Reject order</Text></Pressable></View>
      </View></View>
    </Modal>

    <Modal transparent visible={confirmDelete} animationType="fade" onRequestClose={() => setConfirmDelete(false)}>
      <View style={styles.modalBackdrop}><View style={styles.dialog}>
        <Text style={styles.modalEyebrow}>QUEUE CLEANUP</Text><Text style={styles.modalTitle}>Delete {selected.size} {selected.size === 1 ? 'order' : 'orders'}?</Text>
        <Text style={styles.dialogText}>This removes the selected orders from this local queue. It does not change backend records.</Text>
        <View style={styles.dialogActions}><Pressable accessibilityRole="button" onPress={() => setConfirmDelete(false)}><Text style={styles.toolbarLink}>Cancel</Text></Pressable><Pressable accessibilityRole="button" style={styles.dangerAction} onPress={removeSelected}><Text style={styles.primaryActionText}>Delete selected</Text></Pressable></View>
      </View></View>
    </Modal>
  </SafeAreaView>;
}

function StatusBadge({ status }: { status: RestaurantOrderStatus }) {
  return <View style={[styles.badge, status === 'placed' && styles.badgePlaced, status === 'ready' && styles.badgeReady]}><Text style={styles.badgeText}>{label(status)}</Text></View>;
}

function Info({ label: field, value }: { label: string; value: string }) {
  return <View style={styles.infoRow}><Text style={styles.infoLabel}>{field}</Text><Text style={styles.infoValue}>{value}</Text></View>;
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#F7F7F5' },
  content: { width: '100%', maxWidth: 1180, alignSelf: 'center', paddingHorizontal: 22, paddingTop: 24, paddingBottom: 80 },
  back: { color: '#5A626B', fontSize: 13, fontWeight: '700', marginBottom: 34 },
  eyebrow: { color: '#A64553', fontSize: 11, letterSpacing: 2, fontWeight: '800' },
  title: { color: '#242930', fontSize: 36, fontWeight: '800', marginTop: 10 },
  subtitle: { color: '#69717A', fontSize: 14, marginTop: 8, marginBottom: 24 },
  filters: { flexGrow: 0, marginBottom: 24 },
  filterContent: { gap: 8, paddingRight: 20 },
  filter: { paddingHorizontal: 14, paddingVertical: 10, borderWidth: 1, borderColor: '#D9DEE2', borderRadius: 7, backgroundColor: '#FFF' },
  filterActive: { backgroundColor: '#28313A', borderColor: '#28313A' },
  filterText: { color: '#48525C', fontSize: 12, fontWeight: '700' },
  filterTextActive: { color: '#FFF' },
  queueToolbar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12, marginBottom: 12 },
  queueCount: { color: '#66707A', fontSize: 12, fontWeight: '700' },
  toolbarLink: { color: '#9F3444', fontSize: 12, fontWeight: '800' },
  deleteButton: { alignSelf: 'flex-end', backgroundColor: '#A83B4B', paddingHorizontal: 16, paddingVertical: 10, borderRadius: 7, marginBottom: 14 },
  deleteButtonText: { color: '#FFF', fontWeight: '800', fontSize: 12 },
  message: { color: '#A83B4B', marginBottom: 12, fontSize: 12 },
  orderRow: { flexDirection: 'row', backgroundColor: '#FFF', borderWidth: 1, borderColor: '#E1E4E6', borderRadius: 10, marginBottom: 10, minHeight: 100 },
  checkArea: { width: 48, alignItems: 'center', justifyContent: 'center', borderRightWidth: 1, borderRightColor: '#EDF0F1' },
  checkbox: { width: 18, height: 18, borderWidth: 1.5, borderColor: '#AAB2B8', backgroundColor: '#EAEDF0', borderRadius: 3, alignItems: 'center', justifyContent: 'center' },
  checkboxChecked: { backgroundColor: '#A83B4B', borderColor: '#A83B4B' },
  checkmark: { color: '#FFF', fontSize: 13, fontWeight: '900', lineHeight: 16 },
  orderBody: { flex: 1, paddingHorizontal: 16, paddingVertical: 15, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 14 },
  orderLines: { flex: 1 },
  additionalLine: { marginTop: 8 },
  itemName: { color: '#273039', fontSize: 15, fontWeight: '800' },
  toppings: { color: '#68737C', fontSize: 12, marginTop: 3 },
  rowEnd: { alignItems: 'flex-end', gap: 9 },
  openHint: { color: '#8B949B', fontSize: 11 },
  badge: { alignSelf: 'flex-start', backgroundColor: '#EDF0F2', borderRadius: 5, paddingHorizontal: 9, paddingVertical: 5 },
  badgePlaced: { backgroundColor: '#FFF0D8' },
  badgeReady: { backgroundColor: '#DDF3E7' },
  badgeText: { color: '#35414B', fontSize: 11, fontWeight: '800' },
  empty: { backgroundColor: '#FFF', borderWidth: 1, borderColor: '#E1E4E6', borderRadius: 10, padding: 30, marginTop: 10 },
  emptyTitle: { color: '#273039', fontSize: 18, fontWeight: '800' },
  emptyText: { color: '#68737C', fontSize: 13, lineHeight: 20, marginTop: 7 },
  modalBackdrop: { flex: 1, backgroundColor: 'rgba(24,30,35,0.52)', justifyContent: 'center', alignItems: 'center', padding: 16 },
  modalCard: { width: '100%', maxWidth: 620, maxHeight: '92%', backgroundColor: '#FFF', borderRadius: 13 },
  modalContent: { padding: 26 },
  modalHeading: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 15 },
  modalEyebrow: { color: '#9F3444', letterSpacing: 1.6, fontSize: 10, fontWeight: '800' },
  modalTitle: { color: '#273039', fontSize: 25, fontWeight: '800', marginTop: 6 },
  close: { color: '#69737C', fontSize: 28, lineHeight: 30 },
  divider: { height: 1, backgroundColor: '#E8ECEE', marginVertical: 20 },
  infoRow: { flexDirection: 'row', justifyContent: 'space-between', gap: 20, paddingVertical: 6 },
  infoLabel: { width: 106, color: '#68737C', fontSize: 12, fontWeight: '700' },
  infoValue: { flex: 1, color: '#273039', fontSize: 13, textAlign: 'right' },
  sectionTitle: { color: '#273039', fontSize: 16, fontWeight: '800', marginBottom: 6 },
  detailLine: { paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: '#EEF0F1' },
  detailLineHeader: { flexDirection: 'row', justifyContent: 'space-between', gap: 12 },
  detailItemName: { color: '#273039', fontSize: 14, fontWeight: '700', flex: 1 },
  detailPrice: { color: '#273039', fontSize: 14, fontWeight: '800' },
  detailTopping: { color: '#68737C', fontSize: 12, marginTop: 4, marginLeft: 17 },
  totalRow: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 17, marginBottom: 24 },
  totalLabel: { color: '#273039', fontSize: 15, fontWeight: '800' },
  totalValue: { color: '#273039', fontSize: 17, fontWeight: '800' },
  actions: { flexDirection: 'row', gap: 10 },
  primaryAction: { backgroundColor: '#26313A', paddingHorizontal: 17, paddingVertical: 13, borderRadius: 7, alignItems: 'center', marginTop: 10, flexGrow: 1 },
  primaryActionText: { color: '#FFF', fontSize: 12, fontWeight: '800' },
  secondaryAction: { borderWidth: 1, borderColor: '#A83B4B', paddingHorizontal: 17, paddingVertical: 13, borderRadius: 7, alignItems: 'center', marginTop: 10, flexGrow: 1 },
  secondaryActionText: { color: '#A83B4B', fontSize: 12, fontWeight: '800' },
  dialog: { width: '100%', maxWidth: 480, backgroundColor: '#FFF', borderRadius: 13, padding: 26 },
  dialogText: { color: '#68737C', fontSize: 13, lineHeight: 20, marginTop: 14 },
  reasonInput: { borderWidth: 1, borderColor: '#D5DADD', borderRadius: 7, minHeight: 90, padding: 12, textAlignVertical: 'top', marginTop: 15, color: '#273039' },
  dialogActions: { flexDirection: 'row', alignItems: 'center', justifyContent: 'flex-end', gap: 22, marginTop: 23 },
  dangerAction: { backgroundColor: '#A83B4B', paddingHorizontal: 17, paddingVertical: 12, borderRadius: 7 },
  disabledAction: { opacity: 0.45 },
});
