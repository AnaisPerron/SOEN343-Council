/// <reference types="google.maps" />
import { useEffect, useState } from 'react';
import { AdvancedMarker, APIProvider, Map, useMap, useMapsLibrary } from '@vis.gl/react-google-maps';
import { Modal, View, Text } from 'react-native';
import { googleMapsApiKey } from '@/lib/googleMaps';

type Point = { latitude: number; longitude: number };
type RestaurantPoint = Point & { id: number; name: string; status: string };
export type DeliveryMapProps = { address: string; destination: Point | null; restaurants: RestaurantPoint[] };
const mapId = process.env.EXPO_PUBLIC_GOOGLE_MAPS_MAP_ID?.trim() || 'DEMO_MAP_ID';
const point = (value: Point) => ({ lat: value.latitude, lng: value.longitude });
const distanceKm = (a: Point, b: Point) => {
  const rad = Math.PI / 180;
  const dLat = (b.latitude - a.latitude) * rad;
  const dLng = (b.longitude - a.longitude) * rad;
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(a.latitude * rad) * Math.cos(b.latitude * rad) * Math.sin(dLng / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
};

export default function DeliveryMap({ address, destination, restaurants }: DeliveryMapProps) {
  if (!googleMapsApiKey) return <View><Text>Map unavailable: Google Maps is not configured.</Text></View>;
  return <APIProvider apiKey={googleMapsApiKey}><MapContent address={address} destination={destination} restaurants={restaurants} /></APIProvider>;
}

function MapContent({ address, destination, restaurants }: DeliveryMapProps) {
  const [geocoded, setGeocoded] = useState<Point | null>(null);
  const [fullScreen, setFullScreen] = useState(false);
  const geocoding = useMapsLibrary('geocoding');
  useEffect(() => {
    if (!fullScreen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') setFullScreen(false); };
    window.addEventListener('keydown', closeOnEscape);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', closeOnEscape);
    };
  }, [fullScreen]);
  useEffect(() => {
    setGeocoded(null);
    if (destination || !address || !geocoding) return;
    let active = true;
    new geocoding.Geocoder().geocode({ address }).then(({ results }) => {
      const location = results[0]?.geometry.location;
      if (active) setGeocoded(location ? { latitude: location.lat(), longitude: location.lng() } : null);
    }).catch(() => { if (active) setGeocoded(null); });
    return () => { active = false; };
  }, [address, destination, geocoding]);
  const target = destination ?? geocoded;
  const center = target ?? restaurants[0] ?? { latitude: 45.5017, longitude: -73.5673 };
  const map = <MapScene center={center} destination={target} restaurants={restaurants} />;
  return <>
    {!fullScreen && <div style={{ position: 'relative', height: 260, borderRadius: 12, overflow: 'hidden' }}>
      {map}
      <button type="button" onClick={() => setFullScreen(true)} aria-label="View delivery map full screen" style={mapButtonStyle}>
        Full screen
      </button>
    </div>}
    <Modal visible={fullScreen} animationType="fade" onRequestClose={() => setFullScreen(false)}>
      <View style={{ flex: 1, backgroundColor: '#fff' }} accessibilityLabel="Delivery map full screen">
        {map}
        <button type="button" onClick={() => setFullScreen(false)} aria-label="Exit full screen delivery map" style={mapButtonStyle}>
          Exit full screen
        </button>
      </View>
    </Modal>
  </>;
}

const mapButtonStyle: React.CSSProperties = {
  position: 'absolute', top: 12, right: 12, zIndex: 2, border: '1px solid #d5d9df', borderRadius: 8,
  background: '#fff', color: '#302629', padding: '9px 12px', fontSize: 13, fontWeight: 700,
  boxShadow: '0 2px 8px #0003', cursor: 'pointer',
};

function MapScene({ center, destination, restaurants }: { center: Point; destination: Point | null; restaurants: RestaurantPoint[] }) {
  return <>
    <Map mapId={mapId} defaultCenter={point(center)} defaultZoom={12} gestureHandling="cooperative" disableDefaultUI zoomControl style={{ width: '100%', height: '100%' }}>
      <MapGeometry destination={destination} restaurants={restaurants} />
      {destination && <AdvancedMarker position={point(destination)} title="You" anchorLeft="-50%" anchorTop="-100%">
        <LocationPin color="#2563eb" name="You" />
      </AdvancedMarker>}
      {restaurants.map((restaurant) => <AdvancedMarker key={restaurant.id} position={point(restaurant)} title={`${restaurant.name}, ${restaurant.status}`} anchorLeft="-50%" anchorTop="-100%">
        <LocationPin color="#D42F50" name={restaurant.name} status={restaurant.status} />
      </AdvancedMarker>)}
      {destination && restaurants.map((restaurant) => <AdvancedMarker key={`distance-${restaurant.id}`}
        position={{ lat: (destination.latitude + restaurant.latitude) / 2, lng: (destination.longitude + restaurant.longitude) / 2 }}
        title={`Straight line distance from ${restaurant.name}`}>
        <div style={{ background: 'white', borderRadius: 6, padding: '5px 8px', boxShadow: '0 1px 5px #777', fontSize: 12, fontWeight: 700, whiteSpace: 'nowrap' }}>
          {distanceKm(destination, restaurant).toFixed(1)} km
        </div>
      </AdvancedMarker>)}
    </Map>
  </>;
}

function LocationPin({ color, name, status }: { color: string; name: string; status?: string }) {
  return <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3 }}>
    <div style={{ background: '#fff', borderRadius: 8, padding: '6px 9px', boxShadow: '0 2px 8px #0004', textAlign: 'center', whiteSpace: 'nowrap', fontSize: 12, color: '#302629' }}>
      <div style={{ fontWeight: 800 }}>{name}</div>
      {status && <div style={{ color: '#5d6269', fontSize: 11, marginTop: 2 }}>{status.charAt(0).toUpperCase() + status.slice(1)}</div>}
    </div>
    <svg width="32" height="42" viewBox="0 0 32 42" aria-hidden="true" style={{ filter: 'drop-shadow(0 2px 3px #0005)' }}>
      <path d="M16 1C7.7 1 1 7.7 1 16c0 11 15 25 15 25s15-14 15-25C31 7.7 24.3 1 16 1Z" fill={color} stroke="white" strokeWidth="2" />
      <circle cx="16" cy="16" r="5.5" fill="white" />
    </svg>
  </div>;
}

function MapGeometry({ destination, restaurants }: { destination: Point | null; restaurants: RestaurantPoint[] }) {
  const map = useMap();
  useEffect(() => {
    if (!map || !destination) return;
    const bounds = new google.maps.LatLngBounds();
    bounds.extend(point(destination));
    restaurants.forEach((restaurant) => bounds.extend(point(restaurant)));
    map.fitBounds(bounds, 45);
    const lines = restaurants.map((restaurant) => new google.maps.Polyline({
      map, path: [point(restaurant), point(destination)], strokeColor: '#2563eb', strokeOpacity: 1, strokeWeight: 3,
    }));
    return () => lines.forEach((line) => line.setMap(null));
  }, [map, destination, restaurants]);
  return null;
}
