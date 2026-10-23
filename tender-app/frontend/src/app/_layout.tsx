import { Stack } from 'expo-router';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { AuthProvider } from '@/context/AuthContext';
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
    </Stack>
  );
}

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <CartProvider>
          <OrdersProvider>
            <ItemCustomizationProvider>
              <RootNavigator />
            </ItemCustomizationProvider>
          </OrdersProvider>
        </CartProvider>
      </AuthProvider>
    </SafeAreaProvider>
  );
}
