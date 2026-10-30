import { Redirect } from 'expo-router';
import { ActivityIndicator, View } from 'react-native';
import { useAuth } from '@/context/AuthContext';

export default function IndexRoute() {
  const { user, ready } = useAuth();
  if (!ready) return <View style={{ flex: 1, justifyContent: 'center' }}><ActivityIndicator color="#D42F50" /></View>;
  return <Redirect href={user ? user.role === 'customer' ? '/customer/home/home' : '/account' : '/auth/login'} />;
}
