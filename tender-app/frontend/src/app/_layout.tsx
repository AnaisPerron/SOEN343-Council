import { Stack } from 'expo-router';
import { useFonts } from 'expo-font';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { AuthProvider, useAuth } from '@/context/AuthContext';
import { CartProvider } from '@/context/CartContext';
import { CatalogProvider } from '@/context/CatalogContext';
import { ItemCustomizationProvider } from '@/context/ItemCustomizationContext';
import { OrdersProvider } from '@/context/OrdersContext';

function RootNavigator() {
  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="index" />
      <Stack.Screen name="customer" />
      <Stack.Screen name="operator" />
      <Stack.Screen name="restaurant" />
      <Stack.Screen name="courier" />
      <Stack.Screen name="auth/login" />
      <Stack.Screen name="auth/register" />
      <Stack.Screen name="profile" />
      <Stack.Screen name="account" />
      <Stack.Screen name="restaurant/menu" />
      <Stack.Screen name="restaurant/services/incoming-orders" />
    </Stack>
  );
}

function SessionContent() {
  const { user } = useAuth();
  return <CatalogProvider key={user?.id ?? 'signed-out'}>
    <CartProvider key={user?.id ?? 'signed-out'} userId={user?.id}>
      <OrdersProvider key={user?.id ?? 'signed-out'}>
        <ItemCustomizationProvider>
          <RootNavigator />
        </ItemCustomizationProvider>
      </OrdersProvider>
    </CartProvider>
  </CatalogProvider>;
}

export default function RootLayout() {
  const [loaded, error] = useFonts({ LoveLight: require('../../assets/LoveLight-Regular.ttf') });
  if (!loaded && !error) return null;
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <SessionContent />
      </AuthProvider>
    </SafeAreaProvider>
  );
}
