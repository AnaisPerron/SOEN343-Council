import { Modal, Pressable, Text, TouchableOpacity, View } from 'react-native';
import type { User } from '../../../types/users';
import { styles } from './profile.styles';

interface CustomerProfileProps {
  visible: boolean;
  customerUser?: User;
  onClose: () => void;
  onLogout: () => void;
}

export default function CustomerProfile({
  visible,
  customerUser,
  onClose,
  onLogout,
}: CustomerProfileProps) {
  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onClose}
    >
      <Pressable style={styles.modalBackdrop} onPress={onClose}>
        <Pressable style={styles.profileCard} onPress={(e) => e.stopPropagation()}>
          {/* Close (X) button */}
          <TouchableOpacity
            style={styles.closeButton}
            onPress={onClose}
            accessibilityRole="button"
            accessibilityLabel="Close profile"
            hitSlop={8}
            activeOpacity={0.7}
          >
            <Text style={styles.closeButtonText}>×</Text>
          </TouchableOpacity>

          <Text style={styles.profileCardTitle}>Customer Profile</Text>

          <View style={styles.profileDetailRow}>
            <Text style={styles.profileLabel}>User ID:</Text>
            <Text style={styles.profileValue}>#{customerUser?.id ?? 'N/A'}</Text>
          </View>

          <View style={styles.profileDetailRow}>
            <Text style={styles.profileLabel}>Name:</Text>
            <Text style={styles.profileValue}>{customerUser?.name ?? 'Unknown'}</Text>
          </View>

          <View style={styles.profileDetailRow}>
            <Text style={styles.profileLabel}>Email:</Text>
            <Text style={styles.profileValue}>{customerUser?.email ?? 'N/A'}</Text>
          </View>

          <View style={styles.profileDetailRow}>
            <Text style={styles.profileLabel}>Role:</Text>
            <Text style={[styles.profileValue, { textTransform: 'capitalize' }]}>
              {customerUser?.role ?? 'Customer'}
            </Text>
          </View>

          <TouchableOpacity
            style={styles.logoutButton}
            onPress={onLogout}
            activeOpacity={0.85}
          >
            <Text style={styles.logoutButtonText}>Log Out</Text>
          </TouchableOpacity>
        </Pressable>
      </Pressable>
    </Modal>
  );
}