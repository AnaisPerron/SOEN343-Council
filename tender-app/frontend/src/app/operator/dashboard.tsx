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
import { changeRestaurantStatus, resetSystem, reassignOrder } from './operatorApi';
import ResetSystemDialog from './components/ResetSystemDialog';
import ReassignOrderDialog from './components/ReassignOrderDialog';

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
  const operator = toOperatorUser(user);

  const loadPage = useCallback(async (page = 0) => {
    if (!token || changing.current || resettingRef.current || resetComplete) return;
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
  }, [activeFilter, token, signOut, resetComplete]);

  useEffect(() => {
    void loadPage();
    return () => { requestId.current += 1; };
  }, [loadPage]);

  const updateStatus = async (id: number, status: RestaurantStatus) => {
    if (!token || changing.current || loading || resettingRef.current || resetComplete) return;
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

  return (
    <SafeAreaView style={styles.page} edges={['top']}>
      {!resetComplete && <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={[styles.content, { paddingHorizontal: width < 600 ? 20 : 48 }]}>
        <View style={styles.topbar}>
          <TouchableOpacity disabled={busy} style={styles.profile} onPress={() => setShowProfileMenu(true)} accessibilityRole="button">
            <Text style={styles.profileText}>{getOperatorDisplayName(operator)}</Text>
          </TouchableOpacity>
        </View>
        <View style={styles.statsContainer}>
          {tabs.map((tab) => (
            <TouchableOpacity key={tab.kind} style={[styles.statCard, activeFilter === tab.kind && styles.statCardSelected]}
              onPress={() => { if (!changing.current) setActiveFilter(tab.kind); }} disabled={busy} accessibilityRole="button"
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
        {!resetting && <CityMap refreshKey={mapRefreshKey} />}
        {notice && <Text accessibilityLiveRegion="polite" style={styles.notice}>{notice}</Text>}
        {error && <Text accessibilityRole="alert" style={styles.error}>{error}</Text>}
        {loading && <ActivityIndicator accessibilityLabel="Loading dashboard" color="#D42F50" />}
        {visible?.kind === 'restaurants' && <RestaurantList restaurants={visible.data.items} onStatusChange={updateStatus} changingId={changingId} disabled={busy} />}
        {visible?.kind === 'orders' && <OrdersList orders={visible.data.items} disabled={busy}
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
    </SafeAreaView>
  );
}
