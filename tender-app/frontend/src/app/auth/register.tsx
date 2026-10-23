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

type FormErrors = {
  name?: string;
  email?: string;
  phone?: string;
  password?: string;
  country?: string;
  city?: string;
  addressLine?: string;
  form?: string;
};

export default function RegisterScreen() {
  const { signUp } = useAuth();
  const router = useRouter();
  const [role, setRole] = useState<Role>('customer');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [country, setCountry] = useState('');
  const [city, setCity] = useState('');
  const [addressLine, setAddressLine] = useState('');
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState(false);
  const canRegister = role === 'customer' || role === 'restaurant';

  const handleSubmit = async () => {
    if (loading || !canRegister) return;
    const next: FormErrors = {};
    if (!name.trim()) next.name = 'Name is required';
    if (!EMAIL_REGEX.test(email.trim())) next.email = 'Enter a valid email address';
    const digits = phone.replace(/\D/g, '');
    if (digits.length < 10 || digits.length > 15) next.phone = 'Enter a valid phone number';
    if (password.length < 8) next.password = 'Password must be at least 8 characters';
    if (!country.trim()) next.country = 'Country is required';
    if (!city.trim()) next.city = 'City is required';
    if (!addressLine.trim()) next.addressLine = 'Address line is required';
    setErrors(next);
    if (Object.keys(next).length) return;

    const details = {
      email: email.trim(),
      phoneNumber: phone.trim(),
      password,
      address: {
        country: country.trim(),
        city: city.trim(),
        addressLine: addressLine.trim(),
      },
    };
    setLoading(true);
    try {
      if (role === 'customer') await signUp({ ...details, role: 'customer', name: name.trim() });
      else await signUp({ ...details, role: 'restaurant', restaurantName: name.trim() });
      router.replace('/auth/login');
    } catch (error) {
      setErrors({ form: error instanceof Error ? error.message : 'Something went wrong' });
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
          <Pressable
            style={styles.backButton}
            onPress={() => router.replace('/auth/login')}
            accessibilityRole="button"
            accessibilityLabel="Back to sign in"
          >
            <Text style={styles.backArrow}>←</Text>
            <Text style={styles.backLabel}>Sign in</Text>
          </Pressable>
          <Text style={styles.title}>Tender</Text>
          <Text style={styles.subtitle}>Create your account</Text>

          <View style={styles.roleRow}>
            {ROLE_OPTIONS.map((option) => {
              const selected = option.value === role;
              const disabled = option.value === 'Operator';
              return (
                <Pressable
                  key={option.value}
                  style={[styles.roleButton, selected && styles.roleButtonSelected, disabled && styles.roleButtonDisabled]}
                  onPress={() => setRole(option.value)}
                  disabled={disabled}
                  accessibilityRole="button"
                  accessibilityState={{ selected, disabled }}
                >
                  <Text style={[styles.roleText, selected && styles.roleTextSelected, disabled && styles.roleTextDisabled]}>{option.label}</Text>
                </Pressable>
              );
            })}
          </View>

          {role === 'courier' ? (
            <Text style={styles.notice}>Courier accounts are not supported yet. Registration is unavailable.</Text>
          ) : (
            <>
              <Text style={styles.label}>{role === 'restaurant' ? 'Restaurant name' : 'Name'}</Text>
              <TextInput style={[styles.input, errors.name && styles.inputError]} value={name} onChangeText={setName} placeholder={role === 'restaurant' ? 'Restaurant name' : 'Your name'} placeholderTextColor="#9ca3af" autoComplete={role === 'restaurant' ? 'off' : 'name'} textContentType={role === 'restaurant' ? 'none' : 'name'} />
              {errors.name && <Text style={styles.error}>{errors.name}</Text>}

              <Text style={styles.label}>Email</Text>
              <TextInput style={[styles.input, errors.email && styles.inputError]} value={email} onChangeText={setEmail} placeholder="you@example.com" placeholderTextColor="#9ca3af" autoCapitalize="none" autoComplete="email" keyboardType="email-address" textContentType="emailAddress" />
              {errors.email && <Text style={styles.error}>{errors.email}</Text>}

              <Text style={styles.label}>Phone number</Text>
              <TextInput style={[styles.input, errors.phone && styles.inputError]} value={phone} onChangeText={setPhone} placeholder="514 555 0123" placeholderTextColor="#9ca3af" autoComplete="tel" keyboardType="phone-pad" textContentType="telephoneNumber" />
              {errors.phone && <Text style={styles.error}>{errors.phone}</Text>}

              <Text style={styles.label}>Password</Text>
              <TextInput style={[styles.input, errors.password && styles.inputError]} value={password} onChangeText={setPassword} placeholder="At least 8 characters" placeholderTextColor="#9ca3af" secureTextEntry autoCapitalize="none" textContentType="newPassword" />
              {errors.password && <Text style={styles.error}>{errors.password}</Text>}

              <Text style={styles.addressHeading}>Address</Text>
              <Text style={styles.label}>Country</Text>
              <TextInput style={[styles.input, errors.country && styles.inputError]} value={country} onChangeText={setCountry} placeholder="Country" placeholderTextColor="#9ca3af" autoComplete="country" />
              {errors.country && <Text style={styles.error}>{errors.country}</Text>}

              <Text style={styles.label}>City</Text>
              <TextInput style={[styles.input, errors.city && styles.inputError]} value={city} onChangeText={setCity} placeholder="City" placeholderTextColor="#9ca3af" />
              {errors.city && <Text style={styles.error}>{errors.city}</Text>}

              <Text style={styles.label}>Address line</Text>
              <TextInput style={[styles.input, errors.addressLine && styles.inputError]} value={addressLine} onChangeText={setAddressLine} placeholder="Street address" placeholderTextColor="#9ca3af" autoComplete="street-address" />
              {errors.addressLine && <Text style={styles.error}>{errors.addressLine}</Text>}

              {errors.form && <Text style={[styles.error, styles.formError]}>{errors.form}</Text>}
              <Pressable style={[styles.submit, loading && styles.submitDisabled]} onPress={handleSubmit} disabled={loading}>
                {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.submitText}>Create account</Text>}
              </Pressable>
            </>
          )}

          <View style={styles.switchRow}>
            <Text style={styles.switchText}>Already have an account?</Text>
            <Link href="/auth/login" style={styles.switchLink}>Sign in</Link>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
