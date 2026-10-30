export function parseDeliveryFee(input: string): number | null {
  const value = input.trim().replace(',', '.');
  if (value === '') return 0;
  if (!/^(?:\d+(?:\.\d{0,2})?|\.\d{1,2})$/.test(value)) return null;
  const amount = Number(value);
  return Number.isFinite(amount) && amount <= 99999999.99 ? amount : null;
}
