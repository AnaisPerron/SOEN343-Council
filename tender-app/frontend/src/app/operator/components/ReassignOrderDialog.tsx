import { ActivityIndicator, Modal, Pressable, StyleSheet, Text, View } from 'react-native';

type Props = {
  order: { id: number; courierId: number } | null;
  submitting: boolean;
  error: string | null;
  onCancel: () => void;
  onConfirm: () => void;
};

export default function ReassignOrderDialog({ order, submitting, error, onCancel, onConfirm }: Props) {
  return (
    <Modal visible={order !== null} transparent animationType="fade" onRequestClose={() => { if (!submitting) onCancel(); }}>
      <View style={styles.overlay}>
        <View style={styles.dialog} accessibilityViewIsModal>
          <Text accessibilityRole="header" style={styles.title}>Reassign order #{order?.id}?</Text>
          <Text style={styles.body}>Use this when courier #{order?.courierId} cannot continue. They will be marked offline,
            and the order will return to dispatch. It may wait in the queue until another courier is available.</Text>
          {error && <Text accessibilityRole="alert" style={styles.error}>{error}</Text>}
          {submitting && <ActivityIndicator accessibilityLabel="Reassigning order" color="#D42F50" />}
          <View style={styles.buttons}>
            <Pressable accessibilityRole="button" style={styles.cancel} disabled={submitting} onPress={onCancel}>
              <Text style={styles.cancelText}>Cancel</Text>
            </Pressable>
            <Pressable accessibilityRole="button" style={[styles.confirm, submitting && styles.disabled]}
              disabled={submitting} accessibilityState={{ disabled: submitting }} onPress={onConfirm}>
              <Text style={styles.confirmText}>{submitting ? 'Reassigning…' : 'Confirm reassignment'}</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: 'rgba(0,0,0,0.45)', padding: 24 },
  dialog: { width: '100%', maxWidth: 460, padding: 24, borderRadius: 16, backgroundColor: '#fff' },
  title: { fontSize: 23, fontWeight: '700', color: '#38252A', marginBottom: 12 },
  body: { fontSize: 16, lineHeight: 24, color: '#6b7280', marginBottom: 20 },
  error: { color: '#b91c1c', marginBottom: 16 },
  buttons: { flexDirection: 'row', justifyContent: 'flex-end', gap: 12, marginTop: 8 },
  cancel: { padding: 12, borderRadius: 8, backgroundColor: '#f3f4f6' },
  cancelText: { fontWeight: '700', color: '#38252A' },
  confirm: { padding: 12, borderRadius: 8, backgroundColor: '#D42F50' },
  confirmText: { fontWeight: '700', color: '#fff' },
  disabled: { opacity: 0.5 },
});
