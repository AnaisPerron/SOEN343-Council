import React, { useState } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, Pressable, StyleSheet, ActivityIndicator,
  KeyboardAvoidingView, Platform, ScrollView, LayoutAnimation, UIManager,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '../auth'; 
 
if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental && !global.nativeFabricUIManager) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}
 
const ROLES = [
  { key: 'user',    label: 'User' },
  { key: 'courier', label: 'Courier' },
  { key: 'admin',   label: 'Admin' },
];
 
const animate = () => LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
 
export default function LoginScreen() {
  const { login } = useAuth();
  const insets = useSafeAreaInsets();
 
  const [menuOpen, setMenuOpen] = useState(false);
  const [panelOpen, setPanelOpen] = useState(false);
  const [role, setRole] = useState('user');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
 
  const current = ROLES.find((r) => r.key === role);
 
  const toggleMenu = () => {
    animate();
    setMenuOpen((v) => !v);
  };
 
  const pickRole = (key) => {
    animate();
    if (key !== role) { setEmail(''); setPassword(''); }
    setRole(key);
    setError('');
    setMenuOpen(false);
    setPanelOpen(true);
  };
 
  const togglePanel = () => {
    animate();
    setPanelOpen((v) => !v);
  };
 
  const onSubmit = async () => {
    setError('');
    if (!email.trim() || !password) return setError('Please enter your email and password.');
    setBusy(true);
    try {
      await login(email.trim(), password, role);
    } catch (e) {
      setError(e.message || 'Login failed.');
    } finally {
      setBusy(false);
    }
  };
 
  return (
    <KeyboardAvoidingView style={s.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={[s.topBar, { paddingTop: insets.top + 8 }]}>
        <TouchableOpacity style={s.menuBtn} onPress={toggleMenu} activeOpacity={0.7}>
          <Text style={s.menuIcon}>{menuOpen ? '\u2715' : '\u2630'}</Text>
        </TouchableOpacity>
        <Text style={s.logoSmall}>Tender</Text>
      </View>
 
      <ScrollView contentContainerStyle={s.container} keyboardShouldPersistTaps="handled">

 
        {panelOpen && (
          <View style={s.card}>
            <TouchableOpacity style={s.cardHeader} onPress={togglePanel} activeOpacity={0.7}>
              <View style={{ flex: 1 }}>
                <Text style={s.heading}>{current.label} login</Text>
                <Text style={s.sub}>{current.hint}</Text>
              </View>
              <Text style={s.chevron}>{'\u25B2'}</Text>
            </TouchableOpacity>
 
            <TextInput
              style={s.input}
              placeholder="Email"
              placeholderTextColor="#999"
              autoCapitalize="none"
              keyboardType="email-address"
              value={email}
              onChangeText={setEmail}
            />
            <TextInput
              style={s.input}
              placeholder="Password"
              placeholderTextColor="#999"
              secureTextEntry
              value={password}
              onChangeText={setPassword}
              onSubmitEditing={onSubmit}
            />
 
            {!!error && <Text style={s.error}>{error}</Text>}
 
            <TouchableOpacity style={[s.button, busy && { opacity: 0.7 }]} onPress={onSubmit} disabled={busy}>
              {busy ? <ActivityIndicator color="#fff" /> : <Text style={s.buttonText}>Log in as {current.label}</Text>}
            </TouchableOpacity>
          </View>
        )}
      </ScrollView>
 
      {menuOpen && (
        <>
          <Pressable style={StyleSheet.absoluteFill} onPress={toggleMenu} />
          <View style={[s.menu, { top: insets.top + 8 + 48 }]}>
            {ROLES.map((r) => (
              <TouchableOpacity
                key={r.key}
                style={[s.menuItem, r.key === role && panelOpen && s.menuItemActive]}
                onPress={() => pickRole(r.key)}
                activeOpacity={0.7}
              >
                <View>
                  <Text style={s.menuLabel}>{r.label}</Text>
                  <Text style={s.menuHint}>{r.hint}</Text>
                </View>
              </TouchableOpacity>
            ))}
          </View>
        </>
      )}
    </KeyboardAvoidingView>
  );
}
 
const ORANGE = '#FF5A36';
const s = StyleSheet.create({
  flex: { flex: 1, backgroundColor: '#fff' },
 
  topBar: {
    flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingBottom: 8,
    borderBottomWidth: 1, borderBottomColor: '#F0F0F0', backgroundColor: '#fff',
  },
  menuBtn: {
    width: 40, height: 40, borderRadius: 10, alignItems: 'center', justifyContent: 'center',
    backgroundColor: '#FFF1ED',
  },
  menuIcon: { fontSize: 22, color: ORANGE, fontWeight: '700' },
  logoSmall: { marginLeft: 12, fontSize: 20, fontWeight: '800', color: ORANGE },
 
  container: { flexGrow: 1, justifyContent: 'center', padding: 24 },
  logo: { fontSize: 40, fontWeight: '800', color: ORANGE, textAlign: 'center' },
  tagline: { textAlign: 'center', color: '#777', marginBottom: 28 },
  prompt: { textAlign: 'center', color: '#999' },
 
  card: {
    borderWidth: 1, borderColor: '#EEE', borderRadius: 16, padding: 16, backgroundColor: '#fff',
    shadowColor: '#000', shadowOpacity: 0.06, shadowRadius: 10, shadowOffset: { width: 0, height: 4 },
    elevation: 3,
  },
  cardHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: 14 },
  heading: { fontSize: 20, fontWeight: '700', color: '#222' },
  sub: { color: '#888', marginTop: 2 },
  chevron: { fontSize: 16, color: '#999', paddingLeft: 12 },
 
  input: {
    borderWidth: 1, borderColor: '#DDD', borderRadius: 12, padding: 14,
    fontSize: 16, marginBottom: 12, color: '#222',
  },
  error: { color: '#D93025', marginBottom: 10 },
  button: { backgroundColor: ORANGE, padding: 16, borderRadius: 12, alignItems: 'center', marginTop: 4 },
  buttonText: { color: '#fff', fontWeight: '700', fontSize: 16 },
 
  menu: {
    position: 'absolute', left: 16, width: 230, backgroundColor: '#fff', borderRadius: 14,
    paddingVertical: 6, borderWidth: 1, borderColor: '#EEE',
    shadowColor: '#000', shadowOpacity: 0.15, shadowRadius: 12, shadowOffset: { width: 0, height: 6 },
    elevation: 8,
  },
  menuItem: { flexDirection: 'row', alignItems: 'center', paddingVertical: 12, paddingHorizontal: 14, gap: 12 },
  menuItemActive: { backgroundColor: '#FFF1ED' },
  menuLabel: { fontWeight: '700', color: '#222', fontSize: 16 },
  menuHint: { color: '#888', fontSize: 12 },
});