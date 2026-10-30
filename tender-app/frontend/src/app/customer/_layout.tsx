import { Redirect, Stack } from 'expo-router';
import { ActivityIndicator, View } from 'react-native';
import { useAuth } from '@/context/AuthContext';

export default function CustomerLayout() {
  const { user, ready } = useAuth();
  if (!ready) return <View style={{ flex: 1, justifyContent: 'center' }}><ActivityIndicator color="#D42F50" /></View>;
  if (!user) return <Redirect href="/auth/login" />;
  if (user.role !== 'customer') return <Redirect href="/account" />;
  return <Stack screenOptions={{ headerShown: false }} />;
}
