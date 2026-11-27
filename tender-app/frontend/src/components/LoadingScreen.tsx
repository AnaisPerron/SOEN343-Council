import { useEffect, useState } from 'react';
import { Animated, Easing, Platform, StyleSheet, Text, View } from 'react-native';
import Svg, { Path } from 'react-native-svg';

// Web has no native animation driver; asking for it there only logs a warning.
const nativeDriver = Platform.OS !== 'web';

const FIRE = '#D42F50';
const PAGE = '#FFFAF9';

// Both paths are drawn in a 120 × 160 box. The inner flame is cut out in the page colour,
// so the fire stays a single colour while still reading as a flame.
const VIEW_W = 120;
const VIEW_H = 160;
// Drawn wider and shorter than the paths' own box for a chunkier fire.
const STRETCH_X = 1.15;
const STRETCH_Y = 0.85;
const FIRE_W = VIEW_W * STRETCH_X;
const FIRE_H = VIEW_H * STRETCH_Y;
const OUTER_FLAME =
  'M62 4 C66 22 80 34 88 50 C92 42 92 34 90 26 C104 42 112 64 110 90 C108 126 88 156 60 156 ' +
  'C32 156 10 134 10 104 C10 82 20 66 30 54 C30 64 34 72 40 76 C38 50 46 24 62 4 Z';
const INNER_FLAME =
  'M60 74 C62 90 80 98 80 118 C80 132 70 142 60 142 C48 142 40 132 40 120 ' +
  'C40 108 50 102 52 92 C56 98 58 104 62 106 C60 96 56 86 60 74 Z';
// Path landmarks, converted to on-screen pixels.
const OUTER_TIP_Y = 4 * STRETCH_Y;
const FIRE_BASE_Y = 156 * STRETCH_Y;
const INNER_BASE: [number, number, number] = [60 * STRETCH_X, 142 * STRETCH_Y, 0];

const STAGE = 220;
const CENTER = STAGE / 2;
// Centre the fire's height on the orbit.
const FIRE_TOP = CENTER - (OUTER_TIP_Y + FIRE_BASE_Y) / 2;
// Distance from the orbit's centre up to the fire's tip, where the drop leaves and rejoins.
const TIP_DISTANCE = CENTER - (FIRE_TOP + OUTER_TIP_Y);

const DROP_SIZE = 16;
const ORBIT_RADIUS = 96;
const CYCLE_MS = 2400;

// Phases of one cycle: the drop pulls off the tip, circles the fire, then sinks back in.
const LEAVE_END = 0.16;
const ORBIT_END = 0.82;
const JOIN_END = 0.96;

/** Loops a 0 → 1 value forever so each piece can interpolate its own keyframes. */
function useLoop(duration: number) {
  const [value] = useState(() => new Animated.Value(0));
  useEffect(() => {
    const loop = Animated.loop(
      Animated.timing(value, { toValue: 1, duration, easing: Easing.linear, useNativeDriver: nativeDriver }),
    );
    loop.start();
    return () => loop.stop();
  }, [value, duration]);
  return value;
}

/** Full-screen loader: a Tender-red fire, with a drop that circles it and rejoins. */
export default function LoadingScreen({ message = 'Warming things up…' }: { message?: string }) {
  const outer = useLoop(1400);
  const inner = useLoop(900);
  const cycle = useLoop(CYCLE_MS);

  const steps = [0, 0.25, 0.5, 0.75, 1];
  // The whole fire breathes and leans from its base; the inner flame flickers faster.
  const outerScaleY = outer.interpolate({ inputRange: steps, outputRange: [1, 1.04, 0.98, 1.03, 1] });
  const outerScaleX = outer.interpolate({ inputRange: steps, outputRange: [1, 0.98, 1.02, 0.99, 1] });
  const outerSkew = outer.interpolate({ inputRange: steps, outputRange: ['0deg', '3deg', '-2deg', '2deg', '0deg'] });
  const innerScaleY = inner.interpolate({ inputRange: steps, outputRange: [1, 1.1, 0.94, 1.06, 1] });
  const innerScaleX = inner.interpolate({ inputRange: steps, outputRange: [1, 0.94, 1.05, 0.97, 1] });
  const innerSkew = inner.interpolate({ inputRange: steps, outputRange: ['0deg', '-5deg', '4deg', '-3deg', '0deg'] });

  // The fire gives a little of itself up when the drop leaves and swells when it comes home.
  const pulse = cycle.interpolate({
    inputRange: [0, LEAVE_END, ORBIT_END, JOIN_END, 1],
    outputRange: [1, 0.96, 0.96, 1.05, 1],
  });

  const orbitAngle = cycle.interpolate({
    inputRange: [0, LEAVE_END, ORBIT_END, 1],
    outputRange: ['0deg', '0deg', '-360deg', '-360deg'],
  });
  const radius = cycle.interpolate({
    inputRange: [0, LEAVE_END, ORBIT_END, JOIN_END, 1],
    outputRange: [TIP_DISTANCE - 8, ORBIT_RADIUS, ORBIT_RADIUS, TIP_DISTANCE - 8, TIP_DISTANCE - 8],
  });
  const dropOffset = Animated.multiply(radius, -1);
  const dropScale = cycle.interpolate({
    inputRange: [0, LEAVE_END, ORBIT_END, JOIN_END, 1],
    outputRange: [0.4, 1, 1, 0.4, 0.4],
  });
  // Keep the drop's tail trailing behind it: downward as it rises, backward as it
  // circles, upward as it sinks into the tip.
  const dropTurn = cycle.interpolate({
    inputRange: [0, LEAVE_END * 0.6, LEAVE_END + 0.04, ORBIT_END - 0.04, ORBIT_END + 0.06, 1],
    outputRange: ['225deg', '225deg', '135deg', '135deg', '45deg', '45deg'],
  });

  return (
    <View style={styles.page} accessibilityRole="progressbar" accessibilityLabel={message}>
      <View style={styles.stage}>
        <Animated.View
          style={[
            styles.fire,
            {
              transformOrigin: 'bottom',
              transform: [{ scale: pulse }, { skewX: outerSkew }, { scaleX: outerScaleX }, { scaleY: outerScaleY }],
            },
          ]}
        >
          <Svg width={FIRE_W} height={FIRE_H} viewBox={`0 0 ${VIEW_W} ${VIEW_H}`} preserveAspectRatio="none">
            <Path d={OUTER_FLAME} fill={FIRE} />
          </Svg>
          <Animated.View
            style={[
              StyleSheet.absoluteFill,
              {
                transformOrigin: INNER_BASE,
                transform: [{ skewX: innerSkew }, { scaleX: innerScaleX }, { scaleY: innerScaleY }],
              },
            ]}
          >
            <Svg width={FIRE_W} height={FIRE_H} viewBox={`0 0 ${VIEW_W} ${VIEW_H}`} preserveAspectRatio="none">
              <Path d={INNER_FLAME} fill={PAGE} />
            </Svg>
          </Animated.View>
        </Animated.View>

        <Animated.View style={[StyleSheet.absoluteFill, { transform: [{ rotate: orbitAngle }] }]}>
          <Animated.View
            style={[styles.drop, { transform: [{ translateY: dropOffset }, { rotate: dropTurn }, { scale: dropScale }] }]}
          >
            <View style={styles.dropShape} />
          </Animated.View>
        </Animated.View>
      </View>
      <Text style={styles.brand}>Tender</Text>
      <Text style={styles.message}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  page: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: PAGE },
  stage: { width: STAGE, height: STAGE },
  fire: { position: 'absolute', width: FIRE_W, height: FIRE_H, left: CENTER - FIRE_W / 2, top: FIRE_TOP },
  drop: {
    position: 'absolute',
    width: DROP_SIZE,
    height: DROP_SIZE,
    left: CENTER - DROP_SIZE / 2,
    top: CENTER - DROP_SIZE / 2,
  },
  // A circle with one sharp corner reads as a drop; rotation points that corner along its tail.
  dropShape: { flex: 1, backgroundColor: FIRE, borderRadius: DROP_SIZE / 2, borderTopLeftRadius: 0 },
  brand: { fontFamily: 'LoveLight', fontSize: 52, color: FIRE, marginTop: 4 },
  message: { fontSize: 14, fontWeight: '600', color: '#887B7E', letterSpacing: 0.4, marginTop: 2 },
});
