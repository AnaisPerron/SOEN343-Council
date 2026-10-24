import { useState } from 'react';
import { ScrollView, Text, TouchableOpacity, useWindowDimensions, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '@/context/AuthContext';
import { styles } from './dashboard.styles';
import { restaurantsMock } from '../../mocks/restaurants.mock';
import RestaurantList from './components/resraurantList'; 
import { ordersMock } from '../../mocks/orders.mock';
import OrdersList from './components/ordersList';
import { customersMock, couriersMock,usersMock } from '../../mocks/users.mock';
import CustomerList from './components/customersList';
import CourrierList from './components/courriersList';
import OperatorProfile from './components/operatorProfile';

type FilterType = 'restaurants' | 'orders' | 'customers' | 'couriers';

export default function OperatorHome() {

  const { width } = useWindowDimensions();
  const compact = width < 600;
  const [activeFilter, setActiveFilter] = useState<FilterType>('restaurants');
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const { user, signOut } = useAuth();
  const operatorId = user?.role === 'Operator' ? user.id : undefined;
  const operator = usersMock.find((u) => u.role === 'operator' && u.id === operatorId);
  const operatorName = operator?.name ?? 'Operator';

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
            <Text style={styles.profileText}>{OperatorProfile?.name ?? 'Operator'}</Text>
            
          </TouchableOpacity>
        </View>
        
        {/* Dashboard Statistics */}
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
            <Text style={styles.statValue}>{restaurantsMock.length}</Text>
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
            <Text style={styles.statValue}>{ordersMock.length}</Text>
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

        
        {activeFilter === 'restaurants' && (
          <RestaurantList restaurants={restaurantsMock} />
        )}
        {activeFilter === 'orders' && (
          <OrdersList orders={ordersMock} />
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
        onClose={() => setShowProfileMenu(false)}
        onLogout={() => {
          setShowProfileMenu(false);
          signOut();
        }}
      />
    </SafeAreaView>
  );
}