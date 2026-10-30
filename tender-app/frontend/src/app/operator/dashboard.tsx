import { useState } from 'react';
import { ScrollView, Text, TouchableOpacity, useWindowDimensions, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '@/context/AuthContext';
import { styles } from './dashboard.styles';
import { ApiError } from '@/api/client';
import { cancelOrder, changeRestaurantStatus, updateOperatorProfile, type ProfileUpdateRequest } from './operatorApi';
import type { User } from '../../types/users';
import type { Restaurant } from '@/types/restaurant';
import { restaurantsMock } from '../../mocks/restaurants.mock';
import RestaurantList from './components/resraurantList'; 
import { ordersMock } from '../../mocks/orders.mock';
import OrdersList from './components/ordersList';
import { customersMock, couriersMock } from '../../mocks/users.mock';
import CustomerList from './components/customersList';
import CourrierList from './components/courriersList';
import OperatorProfile from './components/operatorProfile';
import {
  applyRestaurantStatus,
  getOperatorDisplayName,
  markOrderCancelled,
  profileFromResponse,
  toOperatorUser,
} from './operatorLogic';

type FilterType = 'restaurants' | 'orders' | 'customers' | 'couriers';
type Notice = { kind: 'ok' | 'error'; text: string };

export default function OperatorHome() {

  const { width } = useWindowDimensions();
  const compact = width < 600;
  const [activeFilter, setActiveFilter] = useState<FilterType>('restaurants');
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const { user, token, signOut } = useAuth();
  const [restaurants, setRestaurants] = useState(restaurantsMock);
  const [orders, setOrders] = useState(ordersMock);
  const [busyOrderId, setBusyOrderId] = useState<number | null>(null);
  const [busyRestaurantId, setBusyRestaurantId] = useState<number | null>(null);
  const [notice, setNotice] = useState<Notice | null>(null);
  const [updatedProfile, setUpdatedProfile] = useState<User | undefined>(undefined);
  const operator = updatedProfile ?? toOperatorUser(user);
  const operatorName = getOperatorDisplayName(operator);

  const failure = (e: unknown, fallback: string): Notice => ({
    kind: 'error',
    text: e instanceof ApiError ? e.message : fallback,
  });

  // UC-REG-03: errors are thrown on purpose so the profile form can show them
  const handleSaveProfile = async (request: ProfileUpdateRequest) => {
    if (!token) throw new Error('You are signed out.');
    setUpdatedProfile(profileFromResponse(await updateOperatorProfile(request, token)));
  };

  // UC-ORD-05
  const handleCancelOrder = async (orderId: number) => {
    if (!token) return setNotice({ kind: 'error', text: 'You are signed out.' });
    setBusyOrderId(orderId);
    try {
      await cancelOrder(orderId, token);
      setOrders((current) => markOrderCancelled(current, orderId));
      setNotice({ kind: 'ok', text: `Order #${orderId} cancelled.` });
    } catch (e) {
      setNotice(failure(e, 'Could not cancel the order.'));
    } finally {
      setBusyOrderId(null);
    }
  };

  // UC-ORD-06
  const handleChangeStatus = async (restaurantId: number, status: Restaurant['status']) => {
    if (!token) return setNotice({ kind: 'error', text: 'You are signed out.' });
    setBusyRestaurantId(restaurantId);
    try {
      await changeRestaurantStatus(restaurantId, status, token);
      setRestaurants((current) => applyRestaurantStatus(current, restaurantId, status));
      setNotice({ kind: 'ok', text: `Restaurant ${restaurantId} is now ${status}.` });
    } catch (e) {
      setNotice(failure(e, 'Could not change the restaurant status.'));
    } finally {
      setBusyRestaurantId(null);
    }
  };

  return (
    <SafeAreaView style={styles.page} edges={['top']}>
      <ScrollView
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: compact ? 20 : 48 },
        ]}
        
      >
        <View style={[styles.topbar, compact && styles.mobileTopbar]}>
          <TouchableOpacity 
            style={styles.profile} 
            accessibilityRole="button"
            onPress={() => setShowProfileMenu(true)}
            activeOpacity={0.8}
          >
            <Text style={styles.profileText}>{operatorName}</Text>
            
          </TouchableOpacity>
        </View>
        
        <View style={styles.statsContainer}>
          
          <TouchableOpacity 
            style={[
              styles.statCard, 
              styles.restaurantCard,
              activeFilter === 'restaurants' && { borderWidth: 2, borderColor: '#D42F50' }
            ]}
            onPress={() => setActiveFilter('restaurants')}
            activeOpacity={0.7}
          >
            <Text style={styles.statLabel}>Total Restaurants</Text>
            <Text style={styles.statValue}>{restaurants.length}</Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={[
              styles.statCard, 
              styles.ordersCard,
              activeFilter === 'orders' && { borderWidth: 2, borderColor: '#D42F50' }
            ]}
            onPress={() => setActiveFilter('orders')}
            activeOpacity={0.7}
          >
            <Text style={styles.statLabel}>Orders</Text>
            <Text style={styles.statValue}>{orders.length}</Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={[
              styles.statCard, 
              styles.customersCard,
              activeFilter === 'customers' && { borderWidth: 2, borderColor: '#D42F50' }
            ]}
            onPress={() => setActiveFilter('customers')}
            activeOpacity={0.7}
          >
            <Text style={styles.statLabel}>Registered Customers</Text>
            <Text style={styles.statValue}>{customersMock?.length ?? 0}</Text>
           </TouchableOpacity>
          <TouchableOpacity 
            style={[
              styles.statCard, 
              styles.couriersCard,
              activeFilter === 'couriers' && { borderWidth: 2, borderColor: '#D42F50' }
            ]}
            onPress={() => setActiveFilter('couriers')}
            activeOpacity={0.7}
          >
            <Text style={styles.statLabel}>Available Couriers</Text>
            <Text style={styles.statValue}>{couriersMock?.length ?? 0}</Text>
          </TouchableOpacity>
        </View>

        {notice && (
          <Text
            onPress={() => setNotice(null)}
            style={{ color: notice.kind === 'ok' ? '#2E7D32' : '#C62828', fontWeight: '700', marginBottom: 12 }}
          >
            {notice.text}
          </Text>
        )}

        {activeFilter === 'restaurants' && (
          <RestaurantList
            restaurants={restaurants}
            onChangeStatus={handleChangeStatus}
            busyRestaurantId={busyRestaurantId}
          />
        )}
        {activeFilter === 'orders' && (
          <OrdersList orders={orders} onCancel={handleCancelOrder} busyOrderId={busyOrderId} />
        )}
      {activeFilter === 'customers' && (
         <CustomerList customers={customersMock}/>
        )}
        {activeFilter === 'couriers' && (
         <CourrierList courrier={couriersMock}/>
        )}
     

      </ScrollView>
      <OperatorProfile
        visible={showProfileMenu}
        operatorUser={operator}
        onSave={handleSaveProfile}
        onClose={() => setShowProfileMenu(false)}
        onLogout={() => {
          setShowProfileMenu(false);
          signOut();
        }}
      />
    </SafeAreaView>
  );
}