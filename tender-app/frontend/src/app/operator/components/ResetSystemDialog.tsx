import { ActivityIndicator, Modal, Pressable, StyleSheet, Text, View } from 'react-native';

type Props = {
  visible: boolean;
  resetting: boolean;
  completed: boolean;
  error: string | null;
  onCancel: () => void;
  onConfirm: () => void;
  onSignIn: () => void;
};

export default function ResetSystemDialog({ visible, resetting, completed, error, onCancel, onConfirm, onSignIn }: Props) {
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={() => { if (!resetting && !completed) onCancel(); }}>
      <View style={styles.overlay}>
        <View style={styles.dialog} accessibilityViewIsModal>
          <Text accessibilityRole="header" style={styles.title}>{completed ? 'System reset complete' : 'Reset the system?'}</Text>
          <Text style={styles.body}>{completed
            ? 'The initial data has been restored. Sign in again to load the refreshed dashboard.'
            : 'This deletes current data and restores the initial system state. Everyone will be signed out.'}</Text>
          {error && <Text accessibilityRole="alert" style={styles.error}>{error}</Text>}
          {resetting && <ActivityIndicator accessibilityLabel="Resetting system" color="#D42F50" />}
          <View style={styles.buttons}>
            {completed ? (
              <Pressable accessibilityRole="button" style={styles.confirm} onPress={onSignIn}>
                <Text style={styles.confirmText}>Sign in again</Text>
              </Pressable>
            ) : (
              <>
                <Pressable accessibilityRole="button" style={styles.cancel} disabled={resetting} onPress={onCancel}>
                  <Text style={styles.cancelText}>Cancel</Text>
                </Pressable>
                <Pressable accessibilityRole="button" style={[styles.confirm, resetting && styles.disabled]}
                  disabled={resetting} accessibilityState={{ disabled: resetting }} onPress={onConfirm}>
                  <Text style={styles.confirmText}>{resetting ? 'Resetting…' : 'Confirm reset'}</Text>
                </Pressable>
              </>
            )}
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
