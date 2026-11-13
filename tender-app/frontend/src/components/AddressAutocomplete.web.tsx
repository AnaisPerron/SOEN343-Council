/// <reference types="google.maps" />
import { useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { APIProvider, useMapsLibrary } from '@vis.gl/react-google-maps';
import { googleMapsApiKey } from '@/lib/googleMaps';
import type { AddressAutocompleteProps } from './AddressAutocomplete';

export type { AddressAutocompleteProps, AddressLookup, PickedAddress } from './AddressAutocomplete';

// The service runs in Montreal, so nearby addresses come first
const MONTREAL: google.maps.CircleLiteral = { center: { lat: 45.5017, lng: -73.5673 }, radius: 30000 };
const MIN_QUERY = 3;
const DEBOUNCE_MS = 250;

/** Address field that suggests real addresses from Google Places and reports where the picked one is. */
export default function AddressAutocomplete(props: AddressAutocompleteProps) {
  const { value, onChangeText, onPick: _onPick, onLookup: _onLookup, ...inputProps } = props;
  if (!googleMapsApiKey) return <UnavailableInput {...inputProps} value={value} onChangeText={onChangeText} onLookup={_onLookup} />;
  return (
    <APIProvider apiKey={googleMapsApiKey}>
      <SuggestingInput {...props} />
    </APIProvider>
  );
}

function UnavailableInput({ onLookup, ...props }: Omit<AddressAutocompleteProps, 'onPick'>) {
  useEffect(() => { onLookup?.('unavailable'); }, [onLookup]);
  return <View><TextInput {...props} /><Text style={styles.noMatch}>Address verification is unavailable. Configure Google Maps Places to save an address.</Text></View>;
}

function SuggestingInput({ value, onChangeText, onPick, onLookup, ...inputProps }: AddressAutocompleteProps) {
  const places = useMapsLibrary('places');
  const [typing, setTyping] = useState(false);
  const [suggestions, setSuggestions] = useState<google.maps.places.PlacePrediction[]>([]);
  // The text the shown suggestions were found for, so "no match" is only said about the current text
  const [searchedFor, setSearchedFor] = useState<string | null>(null);
  const [lookupError, setLookupError] = useState(false);
  const [hoveredId, setHoveredId] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const requestId = useRef(0);
  // One token per search-and-pick, which Google bills as a single session
  const session = useRef<google.maps.places.AutocompleteSessionToken | null>(null);

  const searching = typing && value.trim().length >= MIN_QUERY;
  // Stale results stay in state but are hidden once the query is too short or an address was picked
  const shown = searching ? suggestions : [];

  useEffect(() => {
    if (places || !searching) return;
    const timer = setTimeout(() => {
      setLookupError(true);
      onLookup?.('unavailable');
    }, 5000);
    return () => clearTimeout(timer);
  }, [places, searching, onLookup, retryCount]);

  useEffect(() => {
    if (!places || !searching) return;
    const currentRequest = ++requestId.current;
    let cancelled = false;
    const timer = setTimeout(async () => {
      session.current ??= new places.AutocompleteSessionToken();
      try {
        const { suggestions: found } = await places.AutocompleteSuggestion.fetchAutocompleteSuggestions({
          input: value,
          sessionToken: session.current,
          locationBias: MONTREAL,
        });
        if (cancelled || currentRequest !== requestId.current) return;
        const predictions = found.flatMap((s) => (s.placePrediction ? [s.placePrediction] : []));
        setSuggestions(predictions);
        setSearchedFor(value);
        setLookupError(false);
        onLookup?.(predictions.length ? 'found' : 'none');
      } catch {
        if (cancelled || currentRequest !== requestId.current) return;
        setSuggestions([]);
        setSearchedFor(null);
        setLookupError(true);
        onLookup?.('unavailable');
      }
    }, DEBOUNCE_MS);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [places, searching, value, onLookup, retryCount]);

  const pick = async (prediction: google.maps.places.PlacePrediction) => {
    requestId.current += 1;
    setTyping(false);
    setSuggestions([]);
    const place = prediction.toPlace();
    try {
      await place.fetchFields({ fields: ['addressComponents', 'location'] });
    } catch {
      setLookupError(true);
      onLookup?.('unavailable');
      return;
    } finally {
      // Fetching the details ends the session
      session.current = null;
    }
    if (!place.location) {
      setLookupError(true);
      onLookup?.('unavailable');
      return;
    }
    const part = (type: string) => place.addressComponents?.find((c) => c.types.includes(type))?.longText ?? '';
    const street = [part('street_number'), part('route')].filter(Boolean).join(' ');
    const city = part('locality') || part('sublocality') || part('administrative_area_level_2');
    const country = part('country');
    if (!part('street_number') || !part('route') || !city || !country) {
      setLookupError(true);
      onLookup?.('none');
      return;
    }
    setLookupError(false);
    onPick({
      addressLine: street,
      city,
      country,
      latitude: place.location.lat(),
      longitude: place.location.lng(),
    });
  };

  return (
    <View>
      <TextInput
        {...inputProps}
        value={value}
        onChangeText={(text) => {
          setTyping(true);
          setLookupError(false);
          setSuggestions([]);
          onChangeText(text);
        }}
      />
      {shown.length > 0 && (
        <View style={styles.list} accessibilityRole="list">
          {shown.map((prediction) => (
            <Pressable
              key={prediction.placeId}
              onPress={() => void pick(prediction)}
              onHoverIn={() => setHoveredId(prediction.placeId)}
              onHoverOut={() => setHoveredId((current) => current === prediction.placeId ? null : current)}
              accessibilityRole="button"
              style={[styles.row, hoveredId === prediction.placeId && styles.rowHovered]}
            >
              <Text style={styles.main}>{prediction.mainText?.text ?? prediction.text.text}</Text>
              {!!prediction.secondaryText?.text && <Text style={styles.secondary}>{prediction.secondaryText.text}</Text>}
            </Pressable>
          ))}
          <Text style={styles.attribution}>Powered by Google</Text>
        </View>
      )}
      {searching && searchedFor === value && shown.length === 0 && (
        <Text style={styles.noMatch}>No matching address found</Text>
      )}
      {lookupError && <View style={styles.retryRow}>
        <Text style={styles.noMatch}>Could not verify this address. Try another suggestion or retry.</Text>
        <Pressable accessibilityRole="button" onPress={() => { setTyping(true); setLookupError(false); setRetryCount((count) => count + 1); }}>
          <Text style={styles.retry}>Retry address search</Text>
        </Pressable>
      </View>}
    </View>
  );
}

const styles = StyleSheet.create({
  list: { marginTop: 4, borderWidth: 1, borderColor: '#d1d5db', borderRadius: 10, backgroundColor: '#fff', overflow: 'hidden' },
  row: { paddingHorizontal: 14, paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  rowHovered: { backgroundColor: '#f9fafb' },
  main: { fontSize: 15, color: '#111827' },
  secondary: { fontSize: 13, color: '#6b7280', marginTop: 2 },
  noMatch: { color: '#6b7280', fontSize: 13, marginTop: 4 },
  attribution: { fontSize: 11, color: '#9ca3af', textAlign: 'right', paddingHorizontal: 14, paddingVertical: 6 },
  retryRow: { marginTop: 4 },
  retry: { color: '#B52F4B', fontSize: 13, fontWeight: '700', marginTop: 6 },
});
