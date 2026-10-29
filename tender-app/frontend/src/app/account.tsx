import { Redirect } from 'expo-router';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AccountMenu from '@/components/AccountMenu';
import { useAuth } from '@/context/AuthContext';

export default function AccountHome() {
  const { user, ready } = useAuth();
  if (!ready) return <SafeAreaView style={styles.page}><ActivityIndicator color="#D42F50" /></SafeAreaView>;
  if (!user) return <Redirect href="/auth/login" />;
  if (user.role === 'customer') return <Redirect href="/customer/home/home" />;
  if (user.role === 'restaurant') return <Redirect href="/restaurant" />;
  return <SafeAreaView style={styles.page}>
    <View style={styles.header}><Text style={styles.brand}>Tender</Text><AccountMenu /></View>
    <View style={styles.content}><Text style={styles.title}>Welcome, {user.name}</Text><Text style={styles.subtitle}>{user.role === 'Operator' ? 'Operator' : 'Courier'} account</Text></View>
  </SafeAreaView>;
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#FFFAF9' },
  header: { height: 70, width: '100%', maxWidth: 1000, alignSelf: 'center', paddingHorizontal: 24, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', zIndex: 10 },
  brand: { color: '#D42F50', fontSize: 25, fontWeight: '900' },
  content: { padding: 24, width: '100%', maxWidth: 1000, alignSelf: 'center' },
  title: { fontSize: 30, fontWeight: '800', color: '#38252A' },
  subtitle: { color: '#79525A', fontSize: 16, marginTop: 8 },
});
