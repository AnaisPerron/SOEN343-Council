import { Redirect, Slot } from 'expo-router';
import { ActivityIndicator, View } from 'react-native';
import { useAuth } from '@/context/AuthContext';
import { homeRoute } from '@/lib/roleRoutes';

export default function OperatorLayout() {
  const { user, ready } = useAuth();
  if (!ready) return <View style={{ flex: 1, justifyContent: 'center' }}><ActivityIndicator color="#D42F50" /></View>;
  if (user?.role !== 'Operator') return <Redirect href={homeRoute(user?.role)} />;
  return <Slot />;
}
