import { Stack } from 'expo-router';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { AuthProvider } from '@/context/AuthContext';
import { CartProvider } from '@/context/CartContext';
import { ItemCustomizationProvider } from '@/context/ItemCustomizationContext';
import { OrdersProvider } from '@/context/OrdersContext';

function RootNavigator() {
  return (
    <Stack screenOptions={{ headerShown: false }}>
      {/*
        TODO: Route through the actual sign-in flow before exposing customer pages.
        For the current restaurant demo, index opens the restaurant display directly
        without an auth guard. Restore protected routes when sign-in is ready.
      */}
      <Stack.Screen name="index" />
      <Stack.Screen name="customer" />
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
