import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Image, Text, TouchableOpacity, View } from 'react-native';
import { capturePayPalOrder, createPayPalOrder, fetchPayPalConfig } from '@/api/paypal';
import type { OrderSubmission, OrderSubmissionResult } from '../services/orderApi';
import { styles } from './confirm-order.styles';

interface PayPalSession {
  start: (options: { presentationMode: 'auto' }, createOrder: Promise<{ orderId: string }>) => Promise<void>;
}
interface PayPalSdk {
  findEligibleMethods: () => Promise<{ isEligible: (method: string) => boolean }>;
  createPayPalOneTimePaymentSession: (callbacks: {
    onApprove: (data: { orderId: string }) => Promise<void>;
    onCancel: () => void;
    onError: (error: unknown) => void;
  }) => Promise<PayPalSession>;
}
interface PayPalWindow extends Window {
  paypal?: { createInstance: (options: { clientId: string; components: string[] }) => Promise<PayPalSdk> };
}

export interface PayPalButtonProps {
  token: string;
  orders: OrderSubmission[];
  disabled: boolean;
  onBegin: () => boolean;
  onSuccess: (receipts: OrderSubmissionResult[]) => void;
  onFailure: (message: string) => void;
  onCancel: () => void;
}

const sdkUrl = 'https://www.sandbox.paypal.com/web-sdk/v6/core';
let scriptPromise: Promise<void> | null = null;
function loadSdk() {
  if ((window as PayPalWindow).paypal) return Promise.resolve();
  if (!scriptPromise) scriptPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = sdkUrl;
    script.onload = () => resolve();
    script.onerror = () => { scriptPromise = null; reject(new Error('Could not load PayPal checkout.')); };
    document.head.appendChild(script);
  });
  return scriptPromise;
}

export default function PayPalButton({ token, orders, disabled, onBegin, onSuccess, onFailure, onCancel }: PayPalButtonProps) {
  const [session, setSession] = useState<PayPalSession | null>(null);
  const [configured, setConfigured] = useState<boolean | null>(null);
  const [loading, setLoading] = useState(false);
  const checkoutOrderId = useRef('');
  const callbacks = useRef({ onSuccess, onFailure, onCancel });
  useEffect(() => {
    callbacks.current = { onSuccess, onFailure, onCancel };
  }, [onSuccess, onFailure, onCancel]);

  useEffect(() => {
    let active = true;
    fetchPayPalConfig(token).then(async (config) => {
      if (!active) return;
      setConfigured(config.enabled);
      if (!config.enabled) return;
      await loadSdk();
      const sdk = await (window as PayPalWindow).paypal!.createInstance({ clientId: config.clientId, components: ['paypal-payments'] });
      if (!active) return;
      if (!(await sdk.findEligibleMethods()).isEligible('paypal')) {
        setConfigured(false);
        return;
      }
      const paymentSession = await sdk.createPayPalOneTimePaymentSession({
        onApprove: async ({ orderId }) => {
          try {
            if (orderId !== checkoutOrderId.current) throw new Error('PayPal approved an unexpected order.');
            callbacks.current.onSuccess(await capturePayPalOrder(token, orderId));
          }
          catch (error) { callbacks.current.onFailure(error instanceof Error ? error.message : 'Could not confirm PayPal payment.'); }
        },
        onCancel: () => callbacks.current.onCancel(),
        onError: (error) => callbacks.current.onFailure(error instanceof Error ? error.message : 'PayPal checkout failed.'),
      });
      if (active) setSession(paymentSession);
    }).catch((error) => { if (active) callbacks.current.onFailure(error instanceof Error ? error.message : 'Could not load PayPal checkout.'); });
    return () => { active = false; };
  }, [token]);

  const start = () => {
    if (!session || loading || disabled || !onBegin()) return;
    setLoading(true);
    // start must run in this click handler; PayPal accepts the order promise while it opens the checkout.
    const createOrder = createPayPalOrder(token, orders).then(({ orderId }) => {
      if (typeof orderId !== 'string' || !orderId) throw new Error('PayPal did not provide a valid order ID.');
      checkoutOrderId.current = orderId;
      return { orderId };
    });
    session.start({ presentationMode: 'auto' }, createOrder)
      .catch((error) => onFailure(error instanceof Error ? error.message : 'PayPal checkout failed.'))
      .finally(() => setLoading(false));
  };

  return <View style={styles.paypalWrap}>
    <TouchableOpacity style={[styles.paypalButton, (disabled || !session || loading) && styles.paypalButtonDisabled]}
      accessibilityRole="button" accessibilityLabel="Pay with PayPal" disabled={disabled || !session || loading} onPress={start}>
      {loading ? <ActivityIndicator color="#003087" /> : <Image source={require('../../../../assets/paypal-logo.png')} style={styles.paypalLogo} resizeMode="contain" />}
      <Text style={styles.paypalButtonText}>{loading ? 'Opening PayPal...' : 'Pay with PayPal'}</Text>
    </TouchableOpacity>
    {configured === false && <Text style={styles.paypalNote}>PayPal sandbox is unavailable until its credentials are configured.</Text>}
  </View>;
}
