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
  // Without a key the field works as a plain input and the account is registered without a location
  if (!googleMapsApiKey) return <TextInput {...inputProps} value={value} onChangeText={onChangeText} />;
  return (
    <APIProvider apiKey={googleMapsApiKey}>
      <SuggestingInput {...props} />
    </APIProvider>
  );
}

function SuggestingInput({ value, onChangeText, onPick, onLookup, ...inputProps }: AddressAutocompleteProps) {
  const places = useMapsLibrary('places');
  const [typing, setTyping] = useState(false);
  const [suggestions, setSuggestions] = useState<google.maps.places.PlacePrediction[]>([]);
  // The text the shown suggestions were found for, so "no match" is only said about the current text
  const [searchedFor, setSearchedFor] = useState<string | null>(null);
  // One token per search-and-pick, which Google bills as a single session
  const session = useRef<google.maps.places.AutocompleteSessionToken | null>(null);

  const searching = typing && value.trim().length >= MIN_QUERY;
  // Stale results stay in state but are hidden once the query is too short or an address was picked
  const shown = searching ? suggestions : [];

  useEffect(() => {
    if (!places || !searching) return;
    let cancelled = false;
    const timer = setTimeout(async () => {
      session.current ??= new places.AutocompleteSessionToken();
      try {
        const { suggestions: found } = await places.AutocompleteSuggestion.fetchAutocompleteSuggestions({
          input: value,
          sessionToken: session.current,
          locationBias: MONTREAL,
          includedRegionCodes: ['ca'],
        });
        if (cancelled) return;
        const predictions = found.flatMap((s) => (s.placePrediction ? [s.placePrediction] : []));
        setSuggestions(predictions);
        setSearchedFor(value);
        onLookup?.(predictions.length ? 'found' : 'none');
      } catch {
        // Places not enabled for the key, or offline: addresses can't be checked, so a typed one is accepted
        if (cancelled) return;
        setSuggestions([]);
        setSearchedFor(null);
        onLookup?.('unavailable');
      }
    }, DEBOUNCE_MS);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [places, searching, value, onLookup]);

  const pick = async (prediction: google.maps.places.PlacePrediction) => {
    setTyping(false);
    setSuggestions([]);
    const place = prediction.toPlace();
    try {
      await place.fetchFields({ fields: ['addressComponents', 'location'] });
    } catch {
      return;
    } finally {
      // Fetching the details ends the session
      session.current = null;
    }
    if (!place.location) return;
    const part = (type: string) => place.addressComponents?.find((c) => c.types.includes(type))?.longText ?? '';
    const street = [part('street_number'), part('route')].filter(Boolean).join(' ');
    onPick({
      addressLine: street || (prediction.mainText?.text ?? prediction.text.text),
      city: part('locality') || part('sublocality') || part('administrative_area_level_2'),
      country: part('country'),
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
          onChangeText(text);
        }}
      />
      {shown.length > 0 && (
        <View style={styles.list} accessibilityRole="list">
          {shown.map((prediction) => (
            <Pressable
              key={prediction.placeId}
              onPress={() => void pick(prediction)}
              accessibilityRole="button"
              style={({ hovered }) => [styles.row, hovered && styles.rowHovered]}
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
});
