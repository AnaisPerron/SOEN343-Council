import { homeRoute } from '@/lib/roleRoutes';
import { Redirect } from 'expo-router';
import { useAuth } from '@/context/AuthContext';
import LoadingScreen from '@/components/LoadingScreen';

export default function IndexRoute() {
  const { user, ready } = useAuth();
  if (!ready) return <LoadingScreen />;
  return <Redirect href={homeRoute(user?.role)} />;
}
