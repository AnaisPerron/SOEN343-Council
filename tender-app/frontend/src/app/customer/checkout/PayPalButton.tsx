import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Image, Text, TouchableOpacity, View } from 'react-native';
import * as WebBrowser from 'expo-web-browser';
import { capturePayPalOrder, createPayPalOrder, fetchPayPalConfig } from '@/api/paypal';
import type { OrderSubmission, OrderSubmissionResult } from '../services/orderApi';
import { styles } from './confirm-order.styles';

export interface PayPalButtonProps {
  token: string;
  orders: OrderSubmission[];
  disabled: boolean;
  onBegin: () => boolean;
  onSuccess: (receipts: OrderSubmissionResult[]) => void;
  onFailure: (message: string) => void;
  onCancel: () => void;
}

const returnUrl = 'tender-mobile-app://customer/checkout/confirm-order';

export default function PayPalButton({ token, orders, disabled, onBegin, onSuccess, onFailure, onCancel }: PayPalButtonProps) {
  const [configured, setConfigured] = useState(false);
  const [loading, setLoading] = useState(false);
  const onFailureRef = useRef(onFailure);

  useEffect(() => { onFailureRef.current = onFailure; }, [onFailure]);

  useEffect(() => {
    let active = true;
    fetchPayPalConfig(token).then((config) => { if (active) setConfigured(config.enabled); })
      .catch((error) => { if (active) onFailureRef.current(error instanceof Error ? error.message : 'Could not load PayPal checkout.'); });
    return () => { active = false; };
  }, [token]);

  const start = async () => {
    if (!configured || disabled || loading || !onBegin()) return;
    setLoading(true);
    try {
      const { orderId, approvalUrl } = await createPayPalOrder(token, orders, returnUrl);
      if (!approvalUrl) throw new Error('PayPal did not provide an approval page.');
      const result = await WebBrowser.openAuthSessionAsync(approvalUrl, returnUrl);
      if (result.type !== 'success') { onCancel(); return; }
      const approvedId = /[?&]token=([^&]+)/.exec(result.url)?.[1];
      if (!approvedId || decodeURIComponent(approvedId) !== orderId) {
        throw new Error('PayPal approval could not be verified.');
      }
      onSuccess(await capturePayPalOrder(token, orderId));
    } catch (error) {
      onFailure(error instanceof Error ? error.message : 'PayPal checkout failed.');
    } finally {
      setLoading(false);
    }
  };

  return <View style={styles.paypalWrap}>
    <TouchableOpacity style={[styles.paypalButton, (!configured || disabled || loading) && styles.paypalButtonDisabled]}
      accessibilityRole="button" accessibilityLabel="Pay with PayPal" disabled={!configured || disabled || loading} onPress={start}>
      {loading ? <ActivityIndicator color="#003087" /> : <Image source={require('../../../../assets/paypal-logo.png')} style={styles.paypalLogo} resizeMode="contain" />}
      <Text style={styles.paypalButtonText}>{loading ? 'Opening PayPal...' : 'Pay with PayPal'}</Text>
    </TouchableOpacity>
    {!configured && <Text style={styles.paypalNote}>PayPal sandbox is unavailable until its credentials are configured.</Text>}
  </View>;
}
