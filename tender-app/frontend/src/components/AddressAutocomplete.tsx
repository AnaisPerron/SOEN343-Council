import { TextInput, type TextInputProps } from 'react-native';

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

// Google Places suggestions need the browser's Maps JavaScript API (see AddressAutocomplete.web.tsx); phones get a plain field
export default function AddressAutocomplete({ value, onChangeText, onPick: _onPick, onLookup: _onLookup, ...inputProps }: AddressAutocompleteProps) {
  return <TextInput {...inputProps} value={value} onChangeText={onChangeText} />;
}
