import type { ComponentRef, ReactNode, RefObject } from 'react';
import {
  Image,
  type ImageSourcePropType,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  ScrollView,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { styles } from './DetailPage.styles';

interface DetailPageProps {
  title: string;
  cover?: ImageSourcePropType;
  coverHeight: number;
  children: ReactNode;
  foreground?: ReactNode;
  scrollRef?: RefObject<ComponentRef<typeof ScrollView> | null>;
  onScroll?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void;
}

export function DetailPage({ title, cover, coverHeight, children, foreground, scrollRef, onScroll }: DetailPageProps) {
  return (
    <View style={styles.page}>
      <View pointerEvents="none" style={[styles.cover, { height: coverHeight }]}>
        {cover ? <Image source={cover} style={styles.coverImage} resizeMode="cover" accessibilityLabel={`${title} cover`} /> : (
          <View style={styles.coverFallback}><Text style={styles.coverFallbackText}>{title}</Text></View>
        )}
      </View>
      <ScrollView ref={scrollRef} style={styles.scroll} contentContainerStyle={styles.scrollContent} scrollEventThrottle={16} onScroll={onScroll}>
        <View pointerEvents="none" style={{ height: coverHeight }} />
        <View style={styles.panel}>{children}</View>
      </ScrollView>
      {foreground}
    </View>
  );
}

export function DetailBackButton({ onPress }: { onPress: () => void }) {
  return (
    <TouchableOpacity accessibilityRole="button" accessibilityLabel="Back to restaurant" onPress={onPress} style={styles.backButton}>
      <Text style={styles.backButtonText}>{'‹'}</Text>
    </TouchableOpacity>
  );
}
