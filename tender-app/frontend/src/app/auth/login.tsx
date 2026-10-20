import { Link } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  LayoutAnimation,
  Platform,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
 
import { ROLE_OPTIONS, useAuth, type Role } from '@/context/AuthContext';
import { authStyles as styles } from './auth.styles';
 
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
 
type FormErrors = { email?: string; password?: string; form?: string };
 
function LoginForm({ role }: { role: Role }) {
  const { signIn } = useAuth();
 
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState(false);
 
  const validate = (): boolean => {
    const next: FormErrors = {};
    if (!EMAIL_REGEX.test(email.trim())) next.email = 'Enter a valid email address';
    if (password.length < 6) next.password = 'Password must be at least 6 characters';
    setErrors(next);
    return Object.keys(next).length === 0;
  };
 
  const handleSubmit = async () => {
    if (loading || !validate()) return;
    setLoading(true);
    try {
      await signIn(email.trim(), password, role);
    } catch (e) {
      setErrors({ form: e instanceof Error ? e.message : 'Something went wrong' });
      setLoading(false);
    }
  };
 
  return (
    <View>
      <Text style={styles.label}>Email</Text>
      <TextInput
        style={[styles.input, errors.email && styles.inputError]}
        value={email}
        onChangeText={setEmail}
        placeholder="you@example.com"
        placeholderTextColor="#9ca3af"
        autoCapitalize="none"
        autoComplete="email"
        keyboardType="email-address"
        textContentType="emailAddress"
      />
      {errors.email && <Text style={styles.error}>{errors.email}</Text>}
 
      <Text style={styles.label}>Password</Text>
      <TextInput
        style={[styles.input, errors.password && styles.inputError]}
        value={password}
        onChangeText={setPassword}
        placeholder="********"
        placeholderTextColor="#9ca3af"
        secureTextEntry
        autoCapitalize="none"
        autoComplete="password"
        textContentType="password"
        onSubmitEditing={handleSubmit}
      />
      {errors.password && <Text style={styles.error}>{errors.password}</Text>}
 
      {errors.form && <Text style={[styles.error, styles.formError]}>{errors.form}</Text>}
 
      <Pressable
        style={[styles.submit, loading && styles.submitDisabled]}
        onPress={handleSubmit}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.submitText}>
            Sign in as {ROLE_OPTIONS.find((r) => r.value === role)?.label}
          </Text>
        )}
      </Pressable>
    </View>
  );
}
 
export default function LoginScreen() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [role, setRole] = useState<Role>('user');
 
  const toggleMenu = () => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setMenuOpen((open) => !open);
  };
 
  return (
    <SafeAreaView style={styles.safe}>
      {/* Top bar */}
      <View style={styles.topBar}>
        <Text style={styles.brand}>Tender</Text>
        <Pressable
          style={[styles.topBarButton, menuOpen && styles.topBarButtonActive]}
          onPress={toggleMenu}
          accessibilityRole="button"
          accessibilityLabel={menuOpen ? 'Close log in menu' : 'Open log in menu'}
          accessibilityState={{ expanded: menuOpen }}
        >
          <Text style={[styles.topBarLabel, menuOpen && styles.topBarLabelActive]}>Log in</Text>
          <Text style={[styles.topBarChevron, menuOpen && styles.topBarLabelActive]}>
          </Text>
        </Pressable>
      </View>
 
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
          {!menuOpen && (
            <View style={styles.hero}>
              <Text style={styles.heroTitle}>Tender</Text>
            </View>
          )}
 
          {menuOpen && (
            <View>
              <View style={styles.roleRow}>
                {ROLE_OPTIONS.map((r) => {
                  const selected = r.value === role;
                  return (
                    <Pressable
                      key={r.value}
                      style={[styles.roleButton, selected && styles.roleButtonSelected]}
                      onPress={() => setRole(r.value)}
                      accessibilityRole="button"
                      accessibilityState={{ selected }}
                    >
                      <Text style={[styles.roleText, selected && styles.roleTextSelected]}>
                        {r.label}
                      </Text>
                    </Pressable>
                  );
                })}
              </View>
 
              <LoginForm key={role} role={role} />
 
              <View style={styles.switchRow}>
                <Text style={styles.switchText}>Don&apos;t have an account?</Text>
                <Link href="/auth/register" style={styles.switchLink}>
                  Register
                </Link>
              </View>
            </View>
          )}
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}