import { useState } from 'react';
import { Modal, Pressable, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { ApiError } from '../../../api/client';
import type { User } from '../../../types/users';
import type { ProfileUpdateRequest } from '../operatorApi';
import { validateProfileUpdate, type ProfileErrors } from '../operatorLogic';
import { styles } from './profile.styles';

interface OperatorProfileProps {
  visible: boolean;
  operatorUser?: User;
  onClose: () => void;
  onLogout: () => void;
  onSave?: (request: ProfileUpdateRequest) => Promise<void>;
}

const inputStyle = {
  borderWidth: 1,
  borderColor: '#DDD',
  borderRadius: 8,
  paddingVertical: 8,
  paddingHorizontal: 10,
  marginTop: 4,
} as const;
const errorStyle = { color: '#C62828', fontSize: 12, marginTop: 4 } as const;

export default function OperatorProfile({ visible, operatorUser, onClose, onLogout, onSave }: OperatorProfileProps) {
  const [editing, setEditing] = useState(false);
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<ProfileErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const startEditing = () => {
    setPhone(operatorUser?.phone ?? '');
    setPassword('');
    setErrors({});
    setServerError(null);
    setEditing(true);
  };

  const save = async () => {
    const result = validateProfileUpdate({ phone, password }, operatorUser);
    if (!result.ok) {
      setErrors(result.errors);
      return;
    }
    setErrors({});
    setServerError(null);
    setSaving(true);
    try {
      await onSave?.(result.request);
      setPassword('');
      setEditing(false);
    } catch (e) {
      setServerError(e instanceof ApiError ? e.message : 'Could not update the profile.');
    } finally {
      setSaving(false);
    }
  };

  const close = () => {
    setEditing(false);
    onClose();
  };

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={close}>
      <Pressable style={styles.modalBackdrop} onPress={close}>
        <Pressable style={styles.profileCard} onPress={(e) => e.stopPropagation()}>
          <TouchableOpacity
            style={styles.closeButton}
            onPress={close}
            accessibilityRole="button"
            accessibilityLabel="Close profile"
            hitSlop={8}
            activeOpacity={0.7}
          >
            <Text style={styles.closeButtonText}>×</Text>
          </TouchableOpacity>

          <Text style={styles.profileCardTitle}>Operator Profile</Text>

          <View style={styles.profileDetailRow}>
            <Text style={styles.profileLabel}>User ID:</Text>
            <Text style={styles.profileValue}>#{operatorUser?.id ?? 'N/A'}</Text>
          </View>

          <View style={styles.profileDetailRow}>
            <Text style={styles.profileLabel}>Name:</Text>
            <Text style={styles.profileValue}>{operatorUser?.name ?? 'Unknown'}</Text>
          </View>

          <View style={styles.profileDetailRow}>
            <Text style={styles.profileLabel}>Email:</Text>
            <Text style={styles.profileValue}>{operatorUser?.email ?? 'N/A'}</Text>
          </View>

          <View style={styles.profileDetailRow}>
            <Text style={styles.profileLabel}>Role:</Text>
            <Text style={[styles.profileValue, { textTransform: 'capitalize' }]}>
              {operatorUser?.role ?? 'Operator'}
            </Text>
          </View>

          {!editing && (
            <>
              <View style={styles.profileDetailRow}>
                <Text style={styles.profileLabel}>Phone:</Text>
                <Text style={styles.profileValue}>{operatorUser?.phone ?? 'N/A'}</Text>
              </View>

              {onSave && (
                <TouchableOpacity accessibilityRole="button" style={{ marginTop: 16, alignItems: 'center' }} onPress={startEditing}>
                  <Text style={{ color: '#D42F50', fontWeight: '700' }}>Edit profile</Text>
                </TouchableOpacity>
              )}
            </>
          )}

          {editing && (
            <View style={{ marginTop: 12 }}>
              <Text style={styles.profileLabel}>Phone</Text>
              <TextInput style={inputStyle} value={phone} onChangeText={setPhone} keyboardType="phone-pad" />
              {errors.phone && <Text style={errorStyle}>{errors.phone}</Text>}

              <Text style={[styles.profileLabel, { marginTop: 12 }]}>New password (leave empty to keep)</Text>
              <TextInput style={inputStyle} value={password} onChangeText={setPassword} secureTextEntry autoCapitalize="none" />
              {errors.password && <Text style={errorStyle}>{errors.password}</Text>}

              {errors.form && <Text style={errorStyle}>{errors.form}</Text>}
              {serverError && <Text style={errorStyle}>{serverError}</Text>}

              <View style={{ flexDirection: 'row', gap: 12, marginTop: 12 }}>
                <TouchableOpacity accessibilityRole="button" disabled={saving} onPress={save}>
                  <Text style={{ color: '#D42F50', fontWeight: '800' }}>{saving ? 'Saving...' : 'Save'}</Text>
                </TouchableOpacity>
                <TouchableOpacity accessibilityRole="button" disabled={saving} onPress={() => setEditing(false)}>
                  <Text style={{ color: '#7A6268', fontWeight: '700' }}>Cancel</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}

          <TouchableOpacity style={styles.logoutButton} onPress={onLogout} activeOpacity={0.85}>
            <Text style={styles.logoutButtonText}>Log Out</Text>
          </TouchableOpacity>
        </Pressable>
      </Pressable>
    </Modal>
  );
}