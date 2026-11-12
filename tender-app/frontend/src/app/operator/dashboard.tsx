import { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, TouchableOpacity, useWindowDimensions, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '@/context/AuthContext';
import { ApiError } from '@/api/client';
import { appendOperatorSection, fetchOperatorSection, fetchOperatorSummary } from '@/lib/operatorDashboard';
import type { OperatorSection, OperatorSectionName, OperatorSummary, RestaurantStatus } from '@/types/operator';
import { styles } from './dashboard.styles';
import RestaurantList from './components/resraurantList';
import OrdersList from './components/ordersList';
import CustomerList from './components/customersList';
import CourrierList from './components/courriersList';
import OperatorProfile from './components/operatorProfile';
import CityMap from '@/components/dash/CityMap';
import { getOperatorDisplayName, toOperatorUser } from './operatorLogic';
import { changeRestaurantStatus, refundOrder, reassignOrder } from './operatorApi';

const tabs: { kind: OperatorSectionName; label: string; count: keyof OperatorSummary }[] = [
  { kind: 'restaurants', label: 'Restaurants', count: 'restaurantCount' },
  { kind: 'orders', label: 'Orders', count: 'orderCount' },
  { kind: 'customers', label: 'Customers', count: 'customerCount' },
  { kind: 'couriers', label: 'Couriers', count: 'courierCount' },
];

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
  const requestId = useRef(0);
  const changing = useRef(false);
  const operator = toOperatorUser(user);

  const loadPage = useCallback(async (page = 0) => {
    if (!token || changing.current) return;
    const version = ++requestId.current;
    setLoading(true);
    setError(null);
    if (page === 0) {
      setSection(null);
      setSummary(null);
    }
    try {
      const [counts, next] = await Promise.all([
        fetchOperatorSummary(token), fetchOperatorSection(activeFilter, page, token),
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
      if (version === requestId.current) setLoading(false);
    }
  }, [activeFilter, token, signOut]);

  useEffect(() => {
    void loadPage();
    return () => { requestId.current += 1; };
  }, [loadPage]);

  const updateStatus = async (id: number, status: RestaurantStatus) => {
    if (!token || changing.current || loading) return;
    changing.current = true;
    const version = ++requestId.current;
    setChangingId(id);
    setError(null);
    try {
      await changeRestaurantStatus(id, status, token);
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

   const runOrderAction = async (
    id: number,
    action: (id: number, token: string) => Promise<unknown>,
    fallback: string,
  ) => {
    if (!token || changing.current || loading) return;
    changing.current = true;
    const version = ++requestId.current;
    setChangingId(id);
    setError(null);
    try {
      await action(id, token);
      setMapRefreshKey((key) => key + 1);
      if (version !== requestId.current) return;
      changing.current = false;
      setChangingId(null);
      await loadPage();
    } catch (cause) {
      if (version !== requestId.current) return;
      if (cause instanceof ApiError && cause.status === 401) signOut();
      else setError(cause instanceof Error ? cause.message : fallback);
    } finally {
      changing.current = false;
      if (version === requestId.current) setChangingId(null);
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
        <CityMap refreshKey={mapRefreshKey} />
        {error && <Text accessibilityRole="alert" style={styles.error}>{error}</Text>}
        {loading && <ActivityIndicator accessibilityLabel="Loading dashboard" color="#D42F50" />}
        {visible?.kind === 'restaurants' && <RestaurantList restaurants={visible.data.items} onStatusChange={updateStatus} changingId={changingId} disabled={busy} />}
        {visible?.kind === 'orders' && (
          <OrdersList
            orders={visible.data.items}
            onRefund={(id) => void runOrderAction(id, refundOrder, 'Unable to refund order')}
            onReassign={(id) => void runOrderAction(id, reassignOrder, 'Unable to reassign courier')}
            changingId={changingId}
            disabled={busy}
          />
        )}
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
