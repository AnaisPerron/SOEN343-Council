import type { CourierState } from '../api/courierDuty.ts';

export const isOnDuty = (state: CourierState) => state !== 'offline';

/** Off duty is refused mid-delivery (R-DLV-03), so the button is disabled while assigned or carrying. */
export const canGoOffDuty = (state: CourierState) => state === 'idle';

export const canGoOnDuty = (state: CourierState) => state === 'offline';

export function stateLabel(state: CourierState) {
  switch (state) {
    case 'offline': return 'Off duty';
    case 'idle': return 'On duty · waiting for an order';
    case 'assigned': return 'Assigned · head to the restaurant';
    case 'carrying': return 'Carrying · on the way to the customer';
  }
}

export const hasActiveDelivery = (state: CourierState) => state === 'assigned' || state === 'carrying';

export const canConfirmPickup = (state: CourierState, orderStatus?: string | null) =>
  state === 'assigned' && orderStatus === 'ready';

export const canConfirmDelivery = (state: CourierState, orderStatus?: string | null) =>
  state === 'carrying' && orderStatus === 'picked_up';
