import { Redirect, Slot } from 'expo-router';
import { useAuth } from '@/context/AuthContext';
import { homeRoute } from '@/lib/roleRoutes';
import LoadingScreen from '@/components/LoadingScreen';

export default function OperatorLayout() {
  const { user, ready } = useAuth();
  if (!ready) return <LoadingScreen />;
  if (user?.role !== 'Operator') return <Redirect href={homeRoute(user?.role)} />;
  return <Slot />;
}
