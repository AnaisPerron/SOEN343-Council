import { homeRoute } from '@/lib/roleRoutes';
import { useState } from 'react';
import { Redirect, useRouter } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AccountMenu from '@/components/AccountMenu';
import { useAuth } from '@/context/AuthContext';
import type { AuthUser } from '@/types/auth';

export default function ProfileScreen() {
  const { user, ready } = useAuth();
  if (!ready) return <SafeAreaView style={styles.page}><ActivityIndicator color="#D42F50" /></SafeAreaView>;
  if (!user) return <Redirect href="/auth/login" />;
  return <ProfileForm key={user.id} user={user} />;
}

function ProfileForm({ user }: { user: AuthUser }) {
  const { saveProfile } = useAuth();
  const router = useRouter();
  const [name, setName] = useState(user.name);
  const [phone, setPhone] = useState(user.phoneNumber);
  const [address, setAddress] = useState(user.address ?? '');
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  const save = async () => {
    if (saving) return;
    if (!name.trim()) { setMessage('Name is required'); return; }
    if (!/^\+?[0-9 ()-]{7,20}$/.test(phone.trim())) { setMessage('Enter a valid phone number'); return; }
    if (user.role === 'restaurant' && !address.trim()) { setMessage('Restaurant address is required'); return; }
    setSaving(true);
    setMessage('');
    try {
      await saveProfile(name.trim(), phone.trim(), user.role === 'restaurant' ? address.trim() : undefined);
      setMessage('Profile saved');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not save profile');
    } finally { setSaving(false); }
  };

  return <SafeAreaView style={styles.page}>
    <View style={styles.header}>
      <Pressable onPress={() => router.canGoBack() ? router.back() : router.replace(homeRoute(user.role))} accessibilityRole="button"><Text style={styles.back}>‹ Back</Text></Pressable>
      <AccountMenu />
    </View>
    <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      <Text style={styles.title}>{user.role === 'restaurant' ? 'Restaurant profile' : 'Profile'}</Text>
      <Text style={styles.subtitle}>{user.role === 'restaurant' ? 'Your restaurant details' : 'Your account details'}</Text>
      <Text style={styles.label}>{user.role === 'restaurant' ? 'Restaurant name' : 'Name'}</Text>
      <TextInput style={styles.input} value={name} onChangeText={setName} accessibilityLabel="Name" autoCapitalize="words" maxLength={100} />
      <Text style={styles.label}>Phone number</Text>
      <TextInput style={styles.input} value={phone} onChangeText={setPhone} accessibilityLabel="Phone number" keyboardType="phone-pad" maxLength={20} />
      {user.role === 'restaurant' ? <>
        <Text style={styles.label}>Restaurant address</Text>
        <TextInput style={styles.input} value={address} onChangeText={setAddress} accessibilityLabel="Restaurant address" maxLength={255} />
      </> : null}
      <Text style={styles.label}>Email</Text>
      <Text style={styles.readonly}>{user.email}</Text>
      {message ? <Text style={styles.message} accessibilityRole="alert">{message}</Text> : null}
      <Pressable style={[styles.save, saving && styles.disabled]} disabled={saving} onPress={save} accessibilityRole="button">
        {saving ? <ActivityIndicator color="#FFFFFF" /> : <Text style={styles.saveText}>Save changes</Text>}
      </Pressable>
    </ScrollView>
  </SafeAreaView>;
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#FFFAF9' },
  header: { height: 64, maxWidth: 800, width: '100%', alignSelf: 'center', paddingHorizontal: 22, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', zIndex: 10 },
  back: { color: '#B52F4B', fontWeight: '700', fontSize: 16 },
  content: { width: '100%', maxWidth: 560, alignSelf: 'center', padding: 24, paddingBottom: 60 },
  title: { fontSize: 32, fontWeight: '800', color: '#38252A' },
  subtitle: { marginTop: 6, marginBottom: 28, color: '#79525A', fontSize: 15 },
  label: { color: '#38252A', fontWeight: '700', marginBottom: 8, marginTop: 18 },
  input: { borderWidth: 1, borderColor: '#E9D7DA', borderRadius: 12, backgroundColor: '#FFFFFF', padding: 14, fontSize: 16, color: '#38252A' },
  readonly: { color: '#79525A', paddingVertical: 14 },
  message: { color: '#B52F4B', marginTop: 20 },
  save: { backgroundColor: '#D42F50', borderRadius: 14, padding: 16, alignItems: 'center', marginTop: 25 },
  disabled: { opacity: 0.65 },
  saveText: { color: '#FFFFFF', fontWeight: '800', fontSize: 16 },
});
