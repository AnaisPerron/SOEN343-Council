import { Text, TextInput, View, type TextInputProps } from 'react-native';
import { useEffect } from 'react';

/** An address picked from the suggestions, split into the registration form's fields. */
export interface PickedAddress {
  addressLine: string;
  city: string;
  country: string;
  latitude: number;
  longitude: number;
}

/** What the last search for the typed text found; 'unavailable' means addresses can't be checked here. */
export type AddressLookup = 'found' | 'none' | 'unavailable';

export type AddressAutocompleteProps = Omit<TextInputProps, 'value' | 'onChangeText'> & {
  value: string;
  /** Typing invalidates any earlier pick, so the form should drop its location here. */
  onChangeText: (text: string) => void;
  onPick: (address: PickedAddress) => void;
  onLookup?: (result: AddressLookup) => void;
};

// Google Places suggestions require the browser Maps JavaScript API.
export default function AddressAutocomplete({ value, onChangeText, onPick: _onPick, onLookup, ...inputProps }: AddressAutocompleteProps) {
  useEffect(() => { onLookup?.('unavailable'); }, [onLookup]);
  return <View><TextInput {...inputProps} value={value} onChangeText={onChangeText} />
    <Text>Address verification is unavailable on this device.</Text></View>;
}
