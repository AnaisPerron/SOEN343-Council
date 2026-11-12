import { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Alert, ScrollView, Text, TextInput, TouchableOpacity, useWindowDimensions, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '@/context/AuthContext';
import { ApiError } from '@/api/client';
import { appendOperatorSection, fetchOperatorSection, fetchOperatorSummary, fetchRestaurantOptions } from '@/lib/operatorDashboard';
import type { OperatorOrder, OperatorOrderFilters, OperatorOrderStatus, OperatorSection, OperatorSectionName, OperatorSummary, RestaurantStatus } from '@/types/operator';
import type { MapRestaurant } from '@/types/dash';
import { styles } from './dashboard.styles';
import RestaurantList from './components/resraurantList';
import OrdersList from './components/ordersList';
import CustomerList from './components/customersList';
import CourrierList from './components/courriersList';
import OperatorProfile from './components/operatorProfile';
import CityMap from '@/components/dash/CityMap';
import { getOperatorDisplayName, toOperatorUser } from './operatorLogic';
import { changeRestaurantStatus, resetDemoSeed } from './operatorApi';

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
const demoSeedResetEnabled = process.env.EXPO_PUBLIC_DEMO_SEED_RESET_ENABLED === 'true';
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
  const [filters, setFilters] = useState<OperatorOrderFilters>(emptyFilters);
  const [restaurantOptions, setRestaurantOptions] = useState<{ id: number; name: string }[]>([]);
  const [fromDraft, setFromDraft] = useState('');
  const [toDraft, setToDraft] = useState('');
  const [mapRestaurant, setMapRestaurant] = useState<MapRestaurant | null>(null);
  const [mapQueue, setMapQueue] = useState<OperatorOrder[]>([]);
  const [mapQueueLoading, setMapQueueLoading] = useState(false);
  const mapQueueRequest = useRef(0);
  const requestId = useRef(0);
  const changing = useRef(false);
  const loadingRef = useRef(false);
  const operator = toOperatorUser(user);

  const loadPage = useCallback(async (page = 0, quiet = false) => {
    if (!token || changing.current || (quiet && loadingRef.current)) return;
    const version = ++requestId.current;
    if (!quiet) { loadingRef.current = true; setLoading(true); setError(null); }
    if (page === 0 && !quiet) {
      setSection(null);
      setSummary(null);
    }
    try {
      const [counts, next] = await Promise.all([
        fetchOperatorSummary(token), fetchOperatorSection(activeFilter, page, token, filters),
      ]);
      if (version !== requestId.current) return;
      setSummary(counts);
      setSection((current) => page === 0 ? next : appendOperatorSection(current, next));
    } catch (cause) {
      if (version !== requestId.current) return;
      if (cause instanceof ApiError && cause.status === 401) {
        setSection(null);
        setSummary(null);
        signOut();
      } else {
        setError(cause instanceof Error ? cause.message : 'Unable to load dashboard');
      }
    } finally {
      if (version === requestId.current && !quiet) { loadingRef.current = false; setLoading(false); }
    }
  }, [activeFilter, token, signOut, filters]);

  useEffect(() => {
    void loadPage();
    return () => { requestId.current += 1; };
  }, [loadPage]);

  useEffect(() => {
    if (!token) return;
    let active = true;
    fetchRestaurantOptions(token).then((options) => { if (active) setRestaurantOptions(options); })
      .catch((cause) => { if (active) setError(cause instanceof Error ? cause.message : 'Unable to load restaurants'); });
    return () => { active = false; };
  }, [token]);

  useEffect(() => {
    if (activeFilter !== 'restaurants') return;
    const timer = setInterval(() => { void loadPage(0, true); }, 15000);
    return () => clearInterval(timer);
  }, [activeFilter, loadPage]);

  const toggleStatus = (status: OperatorOrderStatus) => setFilters((current) => ({ ...current,
    statuses: current.statuses.includes(status) ? current.statuses.filter((item) => item !== status) : [...current.statuses, status],
  }));
  const toggleRestaurant = (id: number) => setFilters((current) => ({ ...current,
    restaurantIds: current.restaurantIds.includes(id) ? current.restaurantIds.filter((item) => item !== id) : [...current.restaurantIds, id],
  }));
  const applyDates = () => {
    const from = fromDraft.trim();
    const to = toDraft.trim();
    if ((from && !validDate(from)) || (to && !validDate(to)) || (from && to && from > to)) {
      setError('Enter valid dates in YYYY-MM-DD format, with From before To.');
      return;
    }
    setError(null);
    setFilters((current) => ({ ...current, from, to }));
  };

  const resetSeed = async () => {
    if (!token || changing.current) return;
    changing.current = true;
    setChangingId(-1);
    setError(null);
    try {
      await resetDemoSeed(token);
      setMapQueue([]);
      setMapRestaurant(null);
      mapQueueRequest.current += 1;
      setMapRefreshKey((key) => key + 1);
      Alert.alert('Demo seed restored', 'Sign in again with the demo operator account to continue.');
      signOut();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to reset the demo seed');
    } finally {
      changing.current = false;
      setChangingId(null);
    }
  };

  const updateStatus = async (id: number, status: RestaurantStatus) => {
    if (!token || changing.current || loading) return;
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

  const openMapRestaurant = async (id: number, restaurant: MapRestaurant) => {
    setMapRestaurant(restaurant);
    setMapQueue([]);
    if (!token) return;
    const request = ++mapQueueRequest.current;
    setMapQueueLoading(true);
    try {
      const result = await fetchOperatorSection('orders', 0, token, {
        statuses: ['accepted', 'preparing'], restaurantIds: [id], from: '', to: '',
      });
      if (request === mapQueueRequest.current && result.kind === 'orders') setMapQueue(result.data.items);
    } catch (cause) {
      if (request === mapQueueRequest.current) setError(cause instanceof Error ? cause.message : 'Unable to load restaurant queue');
    } finally {
      if (request === mapQueueRequest.current) setMapQueueLoading(false);
    }
  };

  const visible = section?.kind === activeFilter ? section : null;
  const busy = loading || changingId !== null;

  return (
    <SafeAreaView style={styles.page} edges={['top']}>
      <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={[styles.content, { paddingHorizontal: width < 600 ? 20 : 48 }]}>
        <View style={styles.topbar}>
          <TouchableOpacity style={styles.profile} onPress={() => setShowProfileMenu(true)} accessibilityRole="button">
            <Text style={styles.profileText}>{getOperatorDisplayName(operator)}</Text>
          </TouchableOpacity>
        </View>
        <View style={styles.statsContainer}>
          {tabs.map((tab) => (
            <TouchableOpacity key={tab.kind} style={[styles.statCard, activeFilter === tab.kind && styles.statCardSelected]}
              onPress={() => { if (!changing.current) setActiveFilter(tab.kind); }} disabled={changingId !== null} accessibilityRole="button"
              accessibilityState={{ selected: activeFilter === tab.kind, disabled: changingId !== null }}>
              <Text style={styles.statLabel}>{tab.label}</Text>
              <Text style={styles.statValue}>{summary?.[tab.count] ?? '—'}</Text>
              {tab.kind === 'couriers' && <Text style={styles.statLabel}>{summary ? `${summary.availableCourierCount} available` : 'Availability unknown'}</Text>}
            </TouchableOpacity>
          ))}
        </View>
        <TouchableOpacity onPress={() => { setMapRefreshKey((key) => key + 1); void loadPage(); }} disabled={busy} accessibilityRole="button" style={styles.action}>
          <Text style={styles.profileText}>Refresh</Text>
        </TouchableOpacity>
        {demoSeedResetEnabled && <TouchableOpacity accessibilityRole="button" disabled={busy} onPress={() => Alert.alert(
          'Reset demo seed?',
          'This restores the demo database to its initial state and removes demo activity since the seed was loaded.',
          [{ text: 'Cancel', style: 'cancel' }, { text: 'Reset seed', style: 'destructive', onPress: () => void resetSeed() }],
        )} style={styles.action}>
          <Text style={styles.profileText}>Reset demo seed</Text>
        </TouchableOpacity>}
        <CityMap refreshKey={mapRefreshKey} onRestaurantPress={openMapRestaurant} />
        {mapRestaurant && <View style={styles.mapDetails}>
          <View style={styles.mapDetailsHeader}>
            <View>
              <Text style={styles.filterTitle}>{mapRestaurant.name} · {mapRestaurant.queueCount} queued</Text>
              <Text style={styles.filterHint}>Preparation queue · {mapRestaurant.status}</Text>
            </View>
            <TouchableOpacity accessibilityRole="button" onPress={() => { setMapRestaurant(null); setMapQueue([]); mapQueueRequest.current += 1; }} style={styles.action}>
              <Text style={styles.profileText}>Close</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.mapQuickActions}>
            <TouchableOpacity accessibilityRole="button" disabled={busy} onPress={() => void updateStatus(mapRestaurant.id, mapRestaurant.status === 'open' ? 'paused' : 'open')} style={styles.action}>
              <Text style={styles.profileText}>{mapRestaurant.status === 'open' ? 'Pause restaurant' : 'Reopen restaurant'}</Text>
            </TouchableOpacity>
          </View>
          <Text style={styles.filterTitle}>Orders being prepared</Text>
          {mapQueueLoading ? <ActivityIndicator accessibilityLabel="Loading restaurant queue" /> : mapQueue.length === 0
            ? <Text style={styles.filterHint}>No accepted or preparing orders.</Text>
            : mapQueue.map((order) => <Text key={order.id} style={styles.mapQueueItem}>Order #{order.id} · {order.status}</Text>)}
          <Text style={styles.filterHint}>Queue size {mapRestaurant.queueCount} · {mapQueue.length} orders shown</Text>
        </View>}
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
        {visible?.kind === 'restaurants' && <RestaurantList restaurants={visible.data.items} onStatusChange={updateStatus} changingId={changingId} disabled={busy} />}
        {visible?.kind === 'orders' && <OrdersList orders={visible.data.items} />}
        {visible?.kind === 'customers' && <CustomerList customers={visible.data.items} />}
        {visible?.kind === 'couriers' && <CourrierList couriers={visible.data.items} />}
        {visible && <Text style={styles.statLabel}>Showing {visible.data.items.length} of {visible.data.total} {activeFilter}</Text>}
        {visible && visible.data.items.length === 0 && !loading && <Text style={styles.notice}>No {activeFilter} found</Text>}
        {visible && (visible.data.page + 1) * visible.data.size < visible.data.total && (
          <TouchableOpacity onPress={() => void loadPage(visible.data.page + 1)} disabled={busy} accessibilityRole="button" style={styles.action}>
            <Text style={styles.profileText}>Load more</Text>
          </TouchableOpacity>
        )}
      </ScrollView>
      <OperatorProfile visible={showProfileMenu} operatorUser={operator} onClose={() => setShowProfileMenu(false)}
        onLogout={() => { setShowProfileMenu(false); signOut(); }} />
    </SafeAreaView>
  );
}
