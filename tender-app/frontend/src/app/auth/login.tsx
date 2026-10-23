import { Link, useRouter } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
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
type SupportedRole = Exclude<Role, 'courier'>;
type FormErrors = { email?: string; password?: string; form?: string };

function LoginForm({ role }: { role: SupportedRole }) {
  const { signIn } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (loading) return;
    const next: FormErrors = {};
    if (!EMAIL_REGEX.test(email.trim())) next.email = 'Enter a valid email address';
    if (!password) next.password = 'Password is required';
    setErrors(next);
    if (Object.keys(next).length) return;

    setLoading(true);
    try {
      await signIn({ role, email: email.trim(), password });
      if (role === 'customer') router.replace('/customer/home/home');
      else setLoading(false);
    } catch (error) {
      setErrors({ form: error instanceof Error ? error.message : 'Something went wrong' });
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
        placeholder="Password"
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
        {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.submitText}>Sign in</Text>}
      </Pressable>
    </View>
  );
}

export default function LoginScreen() {
  const [role, setRole] = useState<Role>('customer');

  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
          <Text style={styles.title}>Tender</Text>
          <Text style={styles.subtitle}>Sign in to your account</Text>

          <View style={styles.roleRow}>
            {ROLE_OPTIONS.map((option) => {
              const selected = option.value === role;
              return (
                <Pressable
                  key={option.value}
                  style={[styles.roleButton, selected && styles.roleButtonSelected]}
                  onPress={() => setRole(option.value)}
                  accessibilityRole="button"
                  accessibilityState={{ selected }}
                >
                  <Text style={[styles.roleText, selected && styles.roleTextSelected]}>{option.label}</Text>
                </Pressable>
              );
            })}
          </View>

          {role === 'courier' ? (
            <Text style={styles.notice}>Courier accounts are not supported yet.</Text>
          ) : (
            <LoginForm key={role} role={role} />
          )}

          {role === 'Operator' ? (
            <Text style={styles.notice}>Operator accounts are assigned. Registration is unavailable.</Text>
          ) : role !== 'courier' ? (
            <View style={styles.switchRow}>
              <Text style={styles.switchText}>Don&apos;t have an account?</Text>
              <Link href="/auth/register" style={styles.switchLink}>Register</Link>
            </View>
          ) : null}
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
