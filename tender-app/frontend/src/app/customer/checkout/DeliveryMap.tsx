import { Text, View } from 'react-native';
export type DeliveryMapProps = { address: string; destination: { latitude: number; longitude: number } | null;
  restaurants: { id: number; name: string; status: string; latitude: number; longitude: number }[] };
export default function DeliveryMap(_props: DeliveryMapProps) {
  return <View><Text>Delivery map is available in the web checkout.</Text></View>;
}
