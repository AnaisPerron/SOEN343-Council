import { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Modal, ScrollView, Text, TextInput, TouchableOpacity, useWindowDimensions, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '@/context/AuthContext';
import { ApiError } from '@/api/client';
import { appendOperatorSection, fetchOperatorSection, fetchOperatorSummary, fetchRestaurantOptions } from '@/lib/operatorDashboard';
import type { OperatorOrderFilters, OperatorOrderStatus, OperatorSection, OperatorSectionName, OperatorSummary, RestaurantStatus } from '@/types/operator';
import type { MapData, MapRestaurant } from '@/types/dash';
import { MONTREAL_MAP_BOUNDS, type MapMarker } from '@/lib/cityMap';
import { styles } from './dashboard.styles';
import RestaurantList from './components/resraurantList';
import OrdersList from './components/ordersList';
import CustomerList from './components/customersList';
import CourrierList from './components/courriersList';
import OperatorProfile from './components/operatorProfile';
import CityMap from '@/components/dash/CityMap';
import MapDirectory from './components/MapDirectory';
import ResetSystemDialog from './components/ResetSystemDialog';
import ReassignOrderDialog from './components/ReassignOrderDialog';
import { getOperatorDisplayName, markOperatorOrderRefunded, toOperatorUser } from './operatorLogic';
import { changeRestaurantStatus, reassignOrder, resetSystem, refundOperatorOrder } from './operatorApi';

const tabs: { kind: OperatorSectionName; label: string; count: keyof OperatorSummary }[] = [
  { kind: 'restaurants', label: 'Restaurants', count: 'restaurantCount' },
  { kind: 'orders', label: 'Orders', count: 'orderCount' },
  { kind: 'customers', label: 'Customers', count: 'customerCount' },
  { kind: 'couriers', label: 'Couriers', count: 'courierCount' },
];

const orderStatuses: { value: OperatorOrderStatus; label: string }[] = [
  { value: 'placed', label: 'Placed' }, { value: 'accepted', label: 'Accepted' },
  { value: 'preparing', label: 'Preparing' }, { value: 'ready', label: 'Ready' },
  { value: 'picked_up', label: 'Picked up' }, { value: 'delivered', label: 'Delivered' },
  { value: 'cancelled', label: 'Cancelled' }, { value: 'rejected', label: 'Rejected' },
  { value: 'refunded', label: 'Refunded' },
];
const emptyFilters: OperatorOrderFilters = { statuses: [], restaurantIds: [], from: '', to: '' };
const validDate = (date: string) => /^\d{4}-\d{2}-\d{2}$/.test(date) && !Number.isNaN(Date.parse(`${date}T00:00:00Z`))
  && new Date(`${date}T00:00:00Z`).toISOString().slice(0, 10) === date;

export default function OperatorHome() {
  const { width } = useWindowDimensions();
  const { user, token, signOut } = useAuth();
  const [activeFilter, setActiveFilter] = useState<OperatorSectionName>('restaurants');
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [summary, setSummary] = useState<OperatorSummary | null>(null);
  const [section, setSection] = useState<OperatorSection | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [changingId, setChangingId] = useState<number | null>(null);
  const [mapRefreshKey, setMapRefreshKey] = useState(0);
  const [mapKinds, setMapKinds] = useState<OperatorSectionName[]>(['restaurants', 'customers', 'couriers']);
  const [directorySections, setDirectorySections] = useState<Partial<Record<OperatorSectionName, OperatorSection>>>({});
  const [directoryDetail, setDirectoryDetail] = useState<{ title: string; detail: string } | null>(null);
  const [filters, setFilters] = useState<OperatorOrderFilters>(emptyFilters);
  const [restaurantOptions, setRestaurantOptions] = useState<{ id: number; name: string }[]>([]);
  const [fromDraft, setFromDraft] = useState('');
  const [toDraft, setToDraft] = useState('');
  const [mapRestaurant, setMapRestaurant] = useState<MapRestaurant | null>(null);
  const [showReset, setShowReset] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [resetComplete, setResetComplete] = useState(false);
  const [resetError, setResetError] = useState<string | null>(null);
  const [reassignTarget, setReassignTarget] = useState<{ id: number; courierId: number } | null>(null);
  const [reassigning, setReassigning] = useState(false);
  const [reassignError, setReassignError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const resettingRef = useRef(false);
  const requestId = useRef(0);
  const changing = useRef(false);
  const loadingRef = useRef(false);
  const dialogOpen = useRef(false);

  useEffect(() => { dialogOpen.current = showReset || reassignTarget !== null; }, [showReset, reassignTarget]);
  const operator = toOperatorUser(user);

  const fetchPage = useCallback(async (page = 0, quiet = false): Promise<void> => {
    if (!token || changing.current || resettingRef.current || resetComplete || (quiet && (loadingRef.current || dialogOpen.current))) return;
    const version = ++requestId.current;
    if (!quiet) {
      loadingRef.current = true;
      setLoading(true);
      setError(null);
      if (page === 0) { setSection(null); setSummary(null); }
    }
    try {
      const [counts, next] = await Promise.all([
        fetchOperatorSummary(token), fetchOperatorSection(activeFilter, page, token, filters),
      ]);
      if (version !== requestId.current) return;
      setSummary(counts);
      setSection((current) => page === 0 ? next : appendOperatorSection(current, next));
    } catch (cause: unknown) {
      if (version !== requestId.current) return;
      if (cause instanceof ApiError && cause.status === 401) {
        setSummary(null);
        setSection(null);
        signOut();
      } else {
        setError(cause instanceof Error ? cause.message : 'Unable to load dashboard');
      }
    } finally {
      if (version === requestId.current && !quiet) { loadingRef.current = false; setLoading(false); }
    }
  }, [activeFilter, token, signOut, filters, resetComplete]);

  const beginLoad = (page = 0) => {
    loadingRef.current = true;
    setLoading(true);
    setError(null);
    if (page === 0) { setSummary(null); setSection(null); }
  };

  const loadPage = useCallback(async (page = 0, quiet = false) => {
    if (!token || changing.current || resettingRef.current || resetComplete) return;
    if (!quiet) beginLoad(page);
    await fetchPage(page, quiet);
  }, [fetchPage, token, resetComplete]);

  useEffect(() => {
    const timer = setTimeout(() => { void fetchPage(); }, 0);
    return () => {
      clearTimeout(timer);
      requestId.current += 1;
    };
  }, [fetchPage]);

  useEffect(() => {
    if (!token || resetting || resetComplete) return;
    let active = true;
    fetchRestaurantOptions(token).then((options) => { if (active) setRestaurantOptions(options); })
      .catch((cause) => { if (active) setError(cause instanceof Error ? cause.message : 'Unable to load restaurants'); });
    return () => { active = false; };
  }, [token, resetting, resetComplete]);

  useEffect(() => {
    if (!token || resetting || resetComplete) return;
    let active = true;
    const refreshDirectory = () => {
      void Promise.all(tabs.map(async ({ kind }) => {
        const first = await fetchOperatorSection(kind, 0, token);
        let combined = first;
        while ((combined.data.page + 1) * combined.data.size < combined.data.total) {
          combined = appendOperatorSection(combined, await fetchOperatorSection(kind, combined.data.page + 1, token));
        }
        return combined;
      })).then((results) => {
        if (active) setDirectorySections(Object.fromEntries(results.map((item) => [item.kind, item])));
      }).catch((cause) => { if (active) setError(cause instanceof Error ? cause.message : 'Unable to load dashboard sections'); });
    };
    refreshDirectory();
    const timer = setInterval(refreshDirectory, 15000);
    return () => { active = false; clearInterval(timer); };
  }, [token, mapRefreshKey, resetting, resetComplete]);

  useEffect(() => {
    const timer = setInterval(() => { void fetchPage(0, true); }, 15000);
    return () => clearInterval(timer);
  }, [fetchPage]);

  const toggleStatus = (status: OperatorOrderStatus) => { beginLoad(); setFilters((current) => ({ ...current,
    statuses: current.statuses.includes(status) ? current.statuses.filter((item) => item !== status) : [...current.statuses, status],
  })); };
  const toggleRestaurant = (id: number) => { beginLoad(); setFilters((current) => ({ ...current,
    restaurantIds: current.restaurantIds.includes(id) ? current.restaurantIds.filter((item) => item !== id) : [...current.restaurantIds, id],
  })); };
  const applyDates = () => {
    const from = fromDraft.trim();
    const to = toDraft.trim();
    if ((from && !validDate(from)) || (to && !validDate(to)) || (from && to && from > to)) {
      setError('Enter valid dates in YYYY-MM-DD format, with From before To.');
      return;
    }
    setError(null);
    beginLoad();
    setFilters((current) => ({ ...current, from, to }));
  };

  const updateStatus = async (id: number, status: RestaurantStatus) => {
    if (!token || changing.current || loading || resettingRef.current || resetComplete) return;
    changing.current = true;
    const version = ++requestId.current;
    setChangingId(id);
    setError(null);
    try {
      await changeRestaurantStatus(id, status, token);
      setMapRestaurant((current) => current?.id === id ? { ...current, status } : current);
      setMapRefreshKey((key) => key + 1);
      if (version !== requestId.current) return;
      changing.current = false;
      setChangingId(null);
      await loadPage();
    } catch (cause) {
      if (version !== requestId.current) return;
      if (cause instanceof ApiError && cause.status === 401) signOut();
      else setError(cause instanceof Error ? cause.message : 'Unable to update restaurant');
    } finally {
      changing.current = false;
      if (version === requestId.current) setChangingId(null);
    }
  };

  const refundOrder = async (orderId: number) => {
    if (!token || changing.current || resettingRef.current || resetComplete) return;
    changing.current = true;
    setChangingId(orderId);
    setError(null);
    setNotice(null);
    try {
      await refundOperatorOrder(orderId, token);
      setSection((current) => markOperatorOrderRefunded(current, orderId, filters.statuses));
      setMapRefreshKey((key) => key + 1);
      setNotice(`Order #${orderId} refunded as Tender Cash.`);
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 401) signOut();
      else setError(cause instanceof Error ? cause.message : 'Unable to refund order');
    } finally {
      changing.current = false;
      setChangingId(null);
    }
  };

  const openMapRestaurant = (_id: number, restaurant: MapRestaurant) => {
    setMapRestaurant(restaurant);
  };
  const updateMapRestaurant = useCallback((data: MapData) => {
    setMapRestaurant((current) => current ? data.restaurants.find((item) => item.id === current.id) ?? current : null);
  }, []);
  const confirmReassignment = async () => {
    if (!token || user?.role !== 'Operator' || !reassignTarget || changing.current || resettingRef.current || resetComplete) return;
    const target = reassignTarget;
    changing.current = true;
    setReassigning(true);
    setReassignError(null);
    setNotice(null);
    const version = ++requestId.current;
    try {
      await reassignOrder(target.id, target.courierId, token);
      if (version !== requestId.current) return;
      changing.current = false;
      setReassigning(false);
      setReassignTarget(null);
      setMapRefreshKey((key) => key + 1);
      setNotice(`Order #${target.id} returned to dispatch. The refreshed list shows its current courier or queued state.`);
      await loadPage();
    } catch (cause) {
      if (version !== requestId.current) return;
      if (cause instanceof ApiError && cause.status === 401) signOut();
      else if (cause instanceof ApiError && (cause.status === 409 || cause.status === 404)) {
        changing.current = false;
        setReassigning(false);
        setReassignTarget(null);
        await loadPage();
        setError(`${cause.message}. Orders have been refreshed; check the current assignment before retrying.`);
      } else setReassignError(cause instanceof Error ? cause.message : 'Unable to reassign the order');
    } finally {
      changing.current = false;
      if (version === requestId.current) setReassigning(false);
    }
  };

  const confirmReset = async () => {
    if (!token || user?.role !== 'Operator' || resettingRef.current || changing.current || resetComplete) return;
    resettingRef.current = true;
    setResetting(true);
    setResetError(null);
    const version = ++requestId.current;
    try {
      await resetSystem(token);
      if (version !== requestId.current) return;
      setSection(null);
      setSummary(null);
      setError(null);
      setNotice(null);
      setReassignTarget(null);
      setChangingId(null);
      setShowProfileMenu(false);
      setActiveFilter('restaurants');
      setMapRefreshKey((key) => key + 1);
      setDirectorySections({});
      setMapRestaurant(null);
      setDirectoryDetail(null);
      setRestaurantOptions([]);
      setFilters(emptyFilters);
      setFromDraft('');
      setToDraft('');
      setResetComplete(true);
    } catch (cause) {
      if (version !== requestId.current) return;
      if (cause instanceof ApiError && cause.status === 401) signOut();
      else setResetError(cause instanceof ApiError && cause.status === 0
        ? 'Connection lost. The reset may have completed. Sign out and sign in again before retrying.'
        : cause instanceof Error ? cause.message : 'Unable to reset the system');
    } finally {
      resettingRef.current = false;
      if (version === requestId.current) setResetting(false);
    }
  };

  const visible = section?.kind === activeFilter ? section : null;
  const busy = loading || changingId !== null || resetting || showReset || reassigning || reassignTarget !== null;
  const queueCounts: Record<number, number> | undefined = directorySections.orders?.kind === 'orders'
    ? directorySections.orders.data.items.reduce<Record<number, number>>((counts, order) => {
      if (order.restaurantId != null && ['placed', 'accepted', 'preparing'].includes(order.status)) {
        counts[order.restaurantId] = (counts[order.restaurantId] ?? 0) + 1;
      }
      return counts;
    }, {}) : undefined;
  const mapQueue = directorySections.orders?.kind === 'orders' && mapRestaurant
    ? directorySections.orders.data.items.filter((order) => order.restaurantId === mapRestaurant.id
      && ['placed', 'accepted', 'preparing'].includes(order.status)) : [];

  return (
    <SafeAreaView style={styles.page} edges={['top']}>
      {!resetComplete && <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={[styles.content, { paddingHorizontal: width < 600 ? 20 : 48 }]}>
        <View style={styles.topbar}>
          <TouchableOpacity style={styles.profile} onPress={() => setShowProfileMenu(true)} accessibilityRole="button">
            <Text style={styles.profileText}>{getOperatorDisplayName(operator)}</Text>
          </TouchableOpacity>
        </View>
        <View style={styles.statsContainer}>
          {tabs.map((tab) => (
            <TouchableOpacity key={tab.kind} style={[styles.statCard, activeFilter === tab.kind && styles.statCardSelected]}
              onPress={() => { if (changing.current || tab.kind === activeFilter) return; beginLoad(); setActiveFilter(tab.kind); }} disabled={busy} accessibilityRole="button"
              accessibilityState={{ selected: activeFilter === tab.kind, disabled: busy }}>
              <Text style={styles.statLabel}>{tab.label}</Text>
              <Text style={styles.statValue}>{summary?.[tab.count] ?? '—'}</Text>
              {tab.kind === 'couriers' && <Text style={styles.statLabel}>{summary ? `${summary.availableCourierCount} available` : 'Availability unknown'}</Text>}
            </TouchableOpacity>
          ))}
        </View>
        <TouchableOpacity onPress={() => { setMapRefreshKey((key) => key + 1); void loadPage(); }} disabled={busy} accessibilityRole="button" style={styles.action}>
          <Text style={styles.profileText}>Refresh</Text>
        </TouchableOpacity>
        {user?.role === 'Operator' && <TouchableOpacity accessibilityRole="button"
          disabled={busy} accessibilityState={{ disabled: busy }} style={styles.action}
          onPress={() => { setResetError(null); setShowReset(true); }}>
          <Text style={styles.profileText}>Reset system</Text>
        </TouchableOpacity>}
        <MapDirectory visible={mapKinds}
          onToggle={(kind) => setMapKinds((current) => current.includes(kind) ? current.filter((item) => item !== kind) : [...current, kind])} />
        <CityMap refreshKey={mapRefreshKey} boundsOverride={MONTREAL_MAP_BOUNDS} onRestaurantPress={openMapRestaurant} onData={updateMapRestaurant} queueCounts={queueCounts}
          visibleKinds={[
            ...(mapKinds.includes('restaurants') ? ['restaurant' as const] : []),
            ...(mapKinds.includes('couriers') ? ['courier' as const] : []),
            ...(mapKinds.includes('customers') ? ['customer' as const] : []),
          ]}
          onMarkerPress={(marker: MapMarker) => { if (marker.kind !== 'restaurant') setDirectoryDetail({ title: marker.name, detail: `${marker.kind} · ${marker.state}` }); }} />
        {mapRestaurant && <View style={styles.mapDetails}>
          <View style={styles.mapDetailsHeader}>
            <View>
              <Text style={styles.filterTitle}>{mapRestaurant.name} · {queueCounts?.[mapRestaurant.id] ?? (queueCounts ? 0 : mapRestaurant.queueCount)} queued</Text>
              <Text style={styles.filterHint}>Preparation queue · {mapRestaurant.status}</Text>
            </View>
            <TouchableOpacity accessibilityRole="button" onPress={() => setMapRestaurant(null)} style={styles.action}>
              <Text style={styles.profileText}>Close</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.mapQuickActions}>
            <TouchableOpacity accessibilityRole="button" disabled={busy} onPress={() => void updateStatus(mapRestaurant.id, mapRestaurant.status === 'open' ? 'paused' : 'open')} style={styles.action}>
              <Text style={styles.profileText}>{mapRestaurant.status === 'open' ? 'Pause restaurant' : 'Reopen restaurant'}</Text>
            </TouchableOpacity>
          </View>
          <Text style={styles.filterTitle}>Queued orders</Text>
          {mapQueue.length === 0
            ? <Text style={styles.filterHint}>No queued orders.</Text>
            : mapQueue.map((order) => <Text key={order.id} style={styles.mapQueueItem}>Order #{order.id} · {order.status}</Text>)}
          <Text style={styles.filterHint}>Queue size {queueCounts?.[mapRestaurant.id] ?? (queueCounts ? 0 : mapRestaurant.queueCount)} · {mapQueue.length} orders shown</Text>
        </View>}
        {notice && <Text accessibilityLiveRegion="polite" style={styles.notice}>{notice}</Text>}
        {error && <Text accessibilityRole="alert" style={styles.error}>{error}</Text>}
        {(activeFilter === 'orders' || activeFilter === 'restaurants') && <View style={styles.filterPanel}>
          <Text style={styles.filterTitle}>Restaurants</Text>
          <Text style={styles.filterHint}>Choose restaurants to show. All are shown when none are selected.</Text>
          <View style={styles.filterChoices}>
            {restaurantOptions.map((restaurant) => <TouchableOpacity key={restaurant.id} accessibilityRole="checkbox"
              accessibilityState={{ checked: filters.restaurantIds.includes(restaurant.id) }}
              onPress={() => toggleRestaurant(restaurant.id)} style={styles.filterChoice}>
              <Text style={styles.filterChoiceText}>{filters.restaurantIds.includes(restaurant.id) ? '☑' : '□'} {restaurant.name}</Text>
            </TouchableOpacity>)}
          </View>
          {activeFilter === 'orders' && <>
            <Text style={styles.filterTitle}>Order status</Text>
            <Text style={styles.filterHint}>All statuses are shown when none are selected.</Text>
            <View style={styles.filterChoices}>
              {orderStatuses.map((status) => <TouchableOpacity key={status.value} accessibilityRole="checkbox"
                accessibilityState={{ checked: filters.statuses.includes(status.value) }}
                onPress={() => toggleStatus(status.value)} style={styles.filterChoice}>
                <Text style={styles.filterChoiceText}>{filters.statuses.includes(status.value) ? '☑' : '□'} {status.label}</Text>
              </TouchableOpacity>)}
            </View>
            <Text style={styles.filterTitle}>Placed date</Text>
            <View style={styles.dateRow}>
              <TextInput accessibilityLabel="From date" placeholder="From YYYY-MM-DD" value={fromDraft}
                onChangeText={setFromDraft} style={styles.dateInput} autoCapitalize="none" />
              <TextInput accessibilityLabel="To date" placeholder="To YYYY-MM-DD" value={toDraft}
                onChangeText={setToDraft} style={styles.dateInput} autoCapitalize="none" />
              <TouchableOpacity accessibilityRole="button" onPress={applyDates} style={styles.action}>
                <Text style={styles.profileText}>Apply dates</Text>
              </TouchableOpacity>
            </View>
          </>}
        </View>}
        {loading && <ActivityIndicator accessibilityLabel="Loading dashboard" color="#D42F50" />}
        {visible?.kind === 'restaurants' && <RestaurantList restaurants={visible.data.items.map((restaurant) => ({
          ...restaurant, queueCount: queueCounts?.[restaurant.id] ?? (queueCounts ? 0 : restaurant.queueCount),
        }))} onStatusChange={updateStatus} changingId={changingId} disabled={busy} />}
        {visible?.kind === 'orders' && <OrdersList orders={visible.data.items} disabled={busy} changingId={changingId}
          onRefund={(orderId) => void refundOrder(orderId)}
          onReassign={user?.role === 'Operator' ? (order) => {
            if (busy || changing.current) return;
            setReassignError(null);
            setReassignTarget(order);
          } : undefined} />}
        {visible?.kind === 'customers' && <CustomerList customers={visible.data.items} />}
        {visible?.kind === 'couriers' && <CourrierList couriers={visible.data.items} />}
        {visible && <Text style={styles.statLabel}>Showing {visible.data.items.length} of {visible.data.total} {activeFilter}</Text>}
        {visible && visible.data.items.length === 0 && !loading && <Text style={styles.notice}>No {activeFilter} found</Text>}
        {visible && (visible.data.page + 1) * visible.data.size < visible.data.total && (
          <TouchableOpacity onPress={() => void loadPage(visible.data.page + 1)} disabled={busy} accessibilityRole="button" style={styles.action}>
            <Text style={styles.profileText}>Load more</Text>
          </TouchableOpacity>
        )}
      </ScrollView>}
      <ReassignOrderDialog order={reassignTarget} submitting={reassigning} error={reassignError}
        onCancel={() => { if (!changing.current) { setReassignTarget(null); setReassignError(null); } }}
        onConfirm={() => void confirmReassignment()} />
      <ResetSystemDialog visible={showReset} resetting={resetting} completed={resetComplete} error={resetError}
        onCancel={() => { if (!resettingRef.current) { setShowReset(false); setResetError(null); } }}
        onConfirm={() => void confirmReset()} onSignIn={signOut} />
      <OperatorProfile visible={showProfileMenu} operatorUser={operator} onClose={() => setShowProfileMenu(false)}
        onLogout={() => { setShowProfileMenu(false); signOut(); }} />
      <Modal visible={directoryDetail !== null} transparent animationType="fade" onRequestClose={() => setDirectoryDetail(null)}>
        <View style={styles.modalBackdrop}><View style={styles.modalCard}>
          <Text style={styles.filterTitle}>{directoryDetail?.title}</Text>
          <Text style={styles.filterHint}>{directoryDetail?.detail}</Text>
          <TouchableOpacity accessibilityRole="button" onPress={() => setDirectoryDetail(null)} style={styles.action}><Text style={styles.profileText}>Close</Text></TouchableOpacity>
        </View></View>
      </Modal>
    </SafeAreaView>
  );
}
