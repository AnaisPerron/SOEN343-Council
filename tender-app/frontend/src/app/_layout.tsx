import { Stack } from 'expo-router';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { AuthProvider, useAuth } from '@/context/AuthContext';
import { CartProvider } from '@/context/CartContext';
import { ItemCustomizationProvider } from '@/context/ItemCustomizationContext';
import { OrdersProvider } from '@/context/OrdersContext';

function RootNavigator() {
  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="index" />
      <Stack.Screen name="customer" />
      <Stack.Screen name="restaurant" />
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
  return <CartProvider key={user?.id ?? 'signed-out'}>
    <OrdersProvider key={user?.id ?? 'signed-out'}>
      <ItemCustomizationProvider>
        <RootNavigator />
      </ItemCustomizationProvider>
    </OrdersProvider>
  </CartProvider>;
}

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <SessionContent />
      </AuthProvider>
    </SafeAreaProvider>
  );
}
