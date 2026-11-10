import { homeRoute } from '@/lib/roleRoutes';
import { useCallback, useState } from 'react';
import { Redirect, useRouter } from 'expo-router';
import { ActivityIndicator, Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AccountMenu from '@/components/AccountMenu';
import { useAuth } from '@/context/AuthContext';
import type { AuthUser } from '@/types/auth';
import AddressAutocomplete, { type AddressLookup, type PickedAddress } from '@/components/AddressAutocomplete';

function splitAddress(value: string | undefined) {
  const parts = (value ?? '').split(',').map((part) => part.trim());
  if (parts.length === 1) return { addressLine: parts[0], city: '', country: '' };
  if (parts.length === 2) return { addressLine: parts[0], city: parts[1], country: '' };
  return {
    addressLine: parts.slice(0, -2).join(', '),
    city: parts[parts.length - 2],
    country: parts[parts.length - 1],
  };
}

export default function ProfileScreen() {
  const { user, ready } = useAuth();
  if (!ready) return <SafeAreaView style={styles.page}><ActivityIndicator color="#D42F50" /></SafeAreaView>;
  if (!user) return <Redirect href="/auth/login" />;
  return <ProfileForm key={user.id} user={user} />;
}

function ProfileForm({ user }: { user: AuthUser }) {
  const { saveProfile, deleteAccount } = useAuth();
  const router = useRouter();
  const [name, setName] = useState(user.name);
  const [phone, setPhone] = useState(user.phoneNumber);
  const initialAddress = splitAddress(user.address);
  const [addressLine, setAddressLine] = useState(initialAddress.addressLine);
  const [city, setCity] = useState(initialAddress.city);
  const [country, setCountry] = useState(initialAddress.country);
  const [location, setLocation] = useState<{ latitude: number; longitude: number } | null>(null);
  const [lookup, setLookup] = useState<AddressLookup | null>(null);
  const onLookup = useCallback((result: AddressLookup) => setLookup(result), []);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const save = async () => {
    if (saving) return;
    if (!name.trim()) { setMessage('Name is required'); return; }
    if (!/^\+?[0-9 ()-]{7,20}$/.test(phone.trim())) { setMessage('Enter a valid phone number'); return; }
    if (user.role === 'restaurant' || user.role === 'customer') {
      if (!addressLine.trim() || !city.trim() || !country.trim()) {
        setMessage('Address line, city, and country are required'); return;
      }
    }
    const address = [addressLine.trim(), city.trim(), country.trim()].join(', ');
    if (address.length > 255) { setMessage('Address must be at most 255 characters'); return; }
    const addressChanged = (user.role === 'restaurant' || user.role === 'customer') && address !== (user.address ?? '').trim();
    if (addressChanged && !location) {
      setMessage(lookup === 'unavailable'
        ? 'Address verification is unavailable. Retry the search before saving.'
        : 'Pick a verified street address from the suggestions before saving.');
      return;
    }
    setSaving(true);
    setMessage('');
    try {
      await saveProfile(name.trim(), phone.trim(),
        user.role === 'restaurant' || user.role === 'customer' ? address.trim() : undefined,
        location ?? undefined);
      setLocation(null);
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
      {user.role === 'restaurant' ? <><Text style={styles.label}>Restaurant name</Text><TextInput style={styles.nameHeading} value={name} onChangeText={setName} accessibilityLabel="Restaurant name" autoCapitalize="words" maxLength={100} /></> : <Text style={styles.title}>Profile</Text>}
      <Text style={styles.subtitle}>{user.role === 'restaurant' ? 'Your restaurant details' : 'Your account details'}</Text>
      {user.role !== 'restaurant' && <><Text style={styles.label}>Name</Text><TextInput style={[styles.input, user.role === 'customer' && styles.customerName]} value={name} onChangeText={setName} accessibilityLabel="Name" autoCapitalize="words" maxLength={100} /></>}
      <Text style={styles.label}>Phone number</Text>
      <TextInput style={styles.input} value={phone} onChangeText={setPhone} accessibilityLabel="Phone number" keyboardType="phone-pad" maxLength={20} />
      {(user.role === 'restaurant' || user.role === 'customer') ? <>
        <Text style={styles.label}>Country</Text>
        <TextInput style={styles.input} value={country} onChangeText={(text) => { setCountry(text); setLocation(null); }}
          accessibilityLabel="Country" autoComplete="country" maxLength={100} />
        <Text style={styles.label}>City</Text>
        <TextInput style={styles.input} value={city} onChangeText={(text) => { setCity(text); setLocation(null); }}
          accessibilityLabel="City" maxLength={100} />
        <Text style={styles.label}>Address line</Text>
        <AddressAutocomplete style={styles.input} value={addressLine} onChangeText={(text) => { setAddressLine(text); setLocation(null); }}
          onPick={(picked: PickedAddress) => {
            setAddressLine(picked.addressLine);
            setCity(picked.city);
            setCountry(picked.country);
            setLocation({ latitude: picked.latitude, longitude: picked.longitude });
            setMessage('');
          }}
          onLookup={onLookup}
          accessibilityLabel="Address line" maxLength={255} autoComplete="street-address" />
      </> : null}
      <Text style={styles.label}>Email</Text>
      <Text style={styles.readonly}>{user.email}</Text>
      {message ? <Text style={styles.message} accessibilityRole="alert">{message}</Text> : null}
      <Pressable style={[styles.save, saving && styles.disabled]} disabled={saving} onPress={save} accessibilityRole="button">
        {saving ? <ActivityIndicator color="#FFFFFF" /> : <Text style={styles.saveText}>Save changes</Text>}
      </Pressable>
      {user.role !== 'Operator' && <View style={styles.dangerSection}>
        <Text style={styles.dangerTitle}>Delete account</Text>
        <Text style={styles.dangerDescription}>Permanently remove your account{user.role === 'customer' ? ' and order history' : ''}. This cannot be undone.</Text>
        <Pressable style={styles.deleteButton} accessibilityRole="button" onPress={() => setConfirmDelete(true)}><Text style={styles.deleteText}>Delete account</Text></Pressable>
      </View>}
    </ScrollView>
    <Modal transparent visible={confirmDelete} animationType="fade" onRequestClose={() => { if (!deleting) setConfirmDelete(false); }}>
      <View style={styles.backdrop}><View style={styles.dialog}>
        <Text style={styles.dangerTitle}>Permanently delete your account?</Text>
        <Text style={styles.dangerDescription}>Your account{user.role === 'customer' ? ' and order history' : ''} will be permanently deleted. You will be signed out.</Text>
        {message ? <Text style={styles.message} accessibilityRole="alert">{message}</Text> : null}
        <View style={styles.dialogActions}>
          <Pressable disabled={deleting} onPress={() => { setConfirmDelete(false); setMessage(''); }}><Text style={styles.back}>Cancel</Text></Pressable>
          <Pressable disabled={deleting} style={[styles.deleteButton, deleting && styles.disabled]} onPress={async () => {
            setDeleting(true); setMessage('');
            try { await deleteAccount(); router.replace('/auth/login'); }
            catch (error) { setMessage(error instanceof Error ? error.message : 'Could not delete account'); setDeleting(false); }
          }}><Text style={styles.deleteText}>{deleting ? 'Deleting…' : 'Delete permanently'}</Text></Pressable>
        </View>
      </View></View>
    </Modal>
  </SafeAreaView>;
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: '#FFFAF9' },
  header: { height: 64, maxWidth: 800, width: '100%', alignSelf: 'center', paddingHorizontal: 22, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', zIndex: 10 },
  back: { color: '#B52F4B', fontWeight: '700', fontSize: 16 },
  content: { width: '100%', maxWidth: 560, alignSelf: 'center', padding: 24, paddingBottom: 60 },
  title: { fontSize: 32, fontWeight: '800', color: '#38252A' },
  nameHeading: { fontFamily: 'LoveLight', fontSize: 42, fontWeight: '400', color: '#38252A', borderBottomWidth: 1, borderBottomColor: '#D9B9BF', paddingVertical: 6 },
  customerName: { fontFamily: 'LoveLight', fontSize: 30, fontWeight: '400' },
  subtitle: { marginTop: 6, marginBottom: 28, color: '#79525A', fontSize: 15 },
  label: { color: '#38252A', fontWeight: '700', marginBottom: 8, marginTop: 18 },
  input: { borderWidth: 1, borderColor: '#E9D7DA', borderRadius: 12, backgroundColor: '#FFFFFF', padding: 14, fontSize: 16, color: '#38252A' },
  readonly: { color: '#79525A', paddingVertical: 14 },
  message: { color: '#B52F4B', marginTop: 20 },
  save: { backgroundColor: '#D42F50', borderRadius: 14, padding: 16, alignItems: 'center', marginTop: 25 },
  disabled: { opacity: 0.65 },
  saveText: { color: '#FFFFFF', fontWeight: '800', fontSize: 16 },
  dangerSection: { marginTop: 40, borderTopWidth: 1, borderColor: '#E9D7DA', paddingTop: 24 },
  dangerTitle: { color: '#78283A', fontSize: 18, fontWeight: '800' },
  dangerDescription: { color: '#79525A', marginTop: 8, lineHeight: 21 },
  deleteButton: { borderWidth: 1, borderColor: '#B52F4B', borderRadius: 10, paddingVertical: 12, paddingHorizontal: 16, alignSelf: 'flex-start', marginTop: 16 },
  deleteText: { color: '#B52F4B', fontWeight: '800' },
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', padding: 24 },
  dialog: { backgroundColor: '#FFF', borderRadius: 16, padding: 24, width: '100%', maxWidth: 480, alignSelf: 'center' },
  dialogActions: { flexDirection: 'row', alignItems: 'center', justifyContent: 'flex-end', gap: 20, marginTop: 12 },
});
