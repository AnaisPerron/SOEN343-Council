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
 
const SIGNUP_ROLES = ROLE_OPTIONS.filter((r) => r.value !== 'admin');
 
type FormErrors = {
  name?: string;
  email?: string;
  phone?: string;
  password?: string;
  form?: string;
};
 
export default function RegisterScreen() {
  const { signUp } = useAuth();
  const router = useRouter();
 
  const [role, setRole] = useState<Role>('user');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState(false);
 
  const validate = (): boolean => {
    const next: FormErrors = {};
    if (!name.trim()) next.name = 'Name is required';
    if (!email.trim()) next.email = 'Email is required';
    else if (!EMAIL_REGEX.test(email.trim())) next.email = 'Enter a valid email address';
    const digits = phone.replace(/\D/g, '');
    if (!phone.trim()) next.phone = 'Phone is required';
    else if (digits.length < 10 || digits.length > 15) next.phone = 'Enter a valid phone number';
    if (!password) next.password = 'Password is required';
    else if (password.length < 6) next.password = 'Password must be at least 6 characters';
    setErrors(next);
    return Object.keys(next).length === 0;
  };
 
  const handleSubmit = async () => {
    if (loading || !validate()) return;
    setLoading(true);
    setErrors({});
    try {
      await signUp(name.trim(), email.trim(), phone.trim(), password, role);
      router.replace('/auth/login');
    } catch (e) {
      setErrors({ form: e instanceof Error ? e.message : 'Something went wrong' });
      setLoading(false);
    }
  };
 
  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
          <Text style={styles.title}>Tender</Text>
          <Text style={styles.subtitle}>Create your account</Text>
 
          <View style={styles.roleRow}>
            {SIGNUP_ROLES.map((r) => {
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
 
          <Text style={styles.label}>Name</Text>
          <TextInput
            style={[styles.input, errors.name && styles.inputError]}
            value={name}
            onChangeText={setName}
            placeholder="Your name"
            placeholderTextColor="#9ca3af"
            autoComplete="name"
            textContentType="name"
          />
          {errors.name && <Text style={styles.error}>{errors.name}</Text>}
 
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
 
          <Text style={styles.label}>Phone</Text>
          <TextInput
            style={[styles.input, errors.phone && styles.inputError]}
            value={phone}
            onChangeText={setPhone}
            placeholder="514 555 0123"
            placeholderTextColor="#9ca3af"
            autoComplete="tel"
            keyboardType="phone-pad"
            textContentType="telephoneNumber"
          />
          {errors.phone && <Text style={styles.error}>{errors.phone}</Text>}
 
          <Text style={styles.label}>Password</Text>
          <TextInput
            style={[styles.input, errors.password && styles.inputError]}
            value={password}
            onChangeText={setPassword}
            placeholder="********"
            placeholderTextColor="#9ca3af"
            secureTextEntry
            autoCapitalize="none"
            textContentType="newPassword"
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
              <Text style={styles.submitText}>Create account</Text>
            )}
          </Pressable>
 
          <View style={styles.switchRow}>
            <Text style={styles.switchText}>Already have an account?</Text>
            <Link href="/auth/login" style={styles.switchLink}>
              Sign in
            </Link>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}