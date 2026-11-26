import { useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { API_URL } from '@/api/client';
import { sendOrderRating } from '@/app/customer/services/orderApi';
import { ratingFormProblem, toRatingSubmission } from '@/app/customer/services/orderRating';

const STARS = [1, 2, 3, 4, 5];
const MAX_COMMENT = 500;

function StarPicker({ label, value, onChange, disabled }: {
  label: string; value: number | null; onChange: (stars: number) => void; disabled: boolean;
}) {
  return <View style={styles.pickerRow}>
    <Text style={styles.pickerLabel}>{label}</Text>
    <View style={styles.stars} accessibilityRole="radiogroup" accessibilityLabel={`${label} rating`}>
      {STARS.map((stars) => <Pressable key={stars} disabled={disabled} onPress={() => onChange(stars)}
        accessibilityRole="radio" accessibilityState={{ checked: value === stars }}
        accessibilityLabel={`${stars} star${stars === 1 ? '' : 's'} for the ${label.toLowerCase()}`}>
        <Text style={[styles.star, value != null && stars <= value && styles.starOn]}>★</Text>
      </Pressable>)}
    </View>
  </View>;
}

/** Rates the restaurant (and courier, when there was one) of a delivered order. */
export default function RatingForm({ orderId, hasCourier, token, onClose }: {
  orderId: string; hasCourier: boolean; token: string; onClose: () => void;
}) {
  const [restaurantStars, setRestaurantStars] = useState<number | null>(null);
  const [courierStars, setCourierStars] = useState<number | null>(null);
  const [comment, setComment] = useState('');
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState<{ kind: 'success' | 'error'; text: string } | null>(null);

  const submit = () => {
    if (sending) return;
    const problem = ratingFormProblem(restaurantStars, courierStars, hasCourier);
    if (problem) { setResult({ kind: 'error', text: problem }); return; }
    setSending(true);
    setResult(null);
    sendOrderRating(`${API_URL}/api/orders/${encodeURIComponent(orderId)}/rating`, token,
      toRatingSubmission(restaurantStars as number, courierStars, comment, hasCourier))
      .then(() => setResult({ kind: 'success', text: 'Thanks! Your rating was saved.' }))
      .catch((error) => setResult({ kind: 'error', text: error instanceof Error ? error.message : 'Could not save your rating.' }))
      .finally(() => setSending(false));
  };

  const saved = result?.kind === 'success';
  return <View style={styles.form}>
    <StarPicker label="Restaurant" value={restaurantStars} onChange={setRestaurantStars} disabled={sending || saved} />
    {hasCourier && <StarPicker label="Courier" value={courierStars} onChange={setCourierStars} disabled={sending || saved} />}
    <TextInput style={styles.comment} value={comment} onChangeText={setComment} editable={!sending && !saved}
      placeholder="Comment (optional)" placeholderTextColor="#9AA2A9" multiline maxLength={MAX_COMMENT}
      accessibilityLabel="Rating comment" />
    {result && <Text style={saved ? styles.success : styles.error} accessibilityRole="alert">{result.text}</Text>}
    <View style={styles.actions}>
      <Pressable onPress={onClose} accessibilityRole="button" style={styles.secondary}>
        <Text style={styles.secondaryText}>{saved ? 'Close' : 'Cancel'}</Text>
      </Pressable>
      {!saved && <Pressable onPress={submit} disabled={sending} accessibilityRole="button" style={styles.primary}>
        {sending ? <ActivityIndicator color="#FFF" /> : <Text style={styles.primaryText}>Submit rating</Text>}
      </Pressable>}
    </View>
  </View>;
}

const styles = StyleSheet.create({
  form: { borderTopWidth: 1, borderTopColor: '#E8ECEE', marginTop: 16, paddingTop: 12 },
  pickerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 },
  pickerLabel: { color: '#38414A', fontWeight: '700', fontSize: 14 },
  stars: { flexDirection: 'row', gap: 6 },
  star: { fontSize: 26, color: '#D3D8DB' }, starOn: { color: '#B97812' },
  comment: { borderWidth: 1, borderColor: '#DDE2E3', borderRadius: 6, padding: 10, minHeight: 60, color: '#273039', textAlignVertical: 'top' },
  success: { color: '#2F7D4A', fontWeight: '700', marginTop: 10 },
  error: { color: '#A83B4B', fontWeight: '700', marginTop: 10 },
  actions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 10, marginTop: 12 },
  secondary: { borderWidth: 1, borderColor: '#C8CDD0', borderRadius: 6, paddingVertical: 10, paddingHorizontal: 14 },
  secondaryText: { color: '#5A626B', fontWeight: '800' },
  primary: { backgroundColor: '#3A9B59', borderRadius: 6, paddingVertical: 10, paddingHorizontal: 14, minWidth: 120, alignItems: 'center' },
  primaryText: { color: '#FFF', fontWeight: '800' },
});
