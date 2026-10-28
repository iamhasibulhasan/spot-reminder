import React, { useEffect, useRef } from 'react';
import { Animated, Easing, StyleSheet, View } from 'react-native';
import { palettes } from '../theme';
import { Logo } from './Logo';

/** Pin drops in with a bounce, radar ripples pulse out, then the title fades up. */
export function AnimatedSplash({ dark, onDone }: { dark: boolean; onDone: () => void }) {
  const drop = useRef(new Animated.Value(0)).current;
  const ripple = useRef(new Animated.Value(0)).current;
  const title = useRef(new Animated.Value(0)).current;
  const out = useRef(new Animated.Value(1)).current;
  const c = dark ? palettes.dark : palettes.light;

  useEffect(() => {
    Animated.loop(
      Animated.timing(ripple, { toValue: 1, duration: 1400, easing: Easing.out(Easing.quad), useNativeDriver: true }),
    ).start();
    Animated.sequence([
      Animated.spring(drop, { toValue: 1, friction: 4, tension: 60, useNativeDriver: true }),
      Animated.timing(title, { toValue: 1, duration: 450, useNativeDriver: true }),
      Animated.delay(550),
      Animated.timing(out, { toValue: 0, duration: 350, useNativeDriver: true }),
    ]).start(onDone);
  }, []);

  const ring = (delay: number) => {
    const p = Animated.modulo(Animated.add(ripple, delay), 1);
    return {
      opacity: p.interpolate({ inputRange: [0, 1], outputRange: [0.5, 0] }),
      transform: [{ scale: p.interpolate({ inputRange: [0, 1], outputRange: [0.6, 2.4] }) }],
    };
  };

  return (
    <Animated.View style={[styles.root, { backgroundColor: c.bg, opacity: out }]}>
      <View style={styles.center}>
        <Animated.View style={[styles.ring, { borderColor: c.primary }, ring(0)]} />
        <Animated.View style={[styles.ring, { borderColor: c.accent }, ring(0.5)]} />
        <Animated.View
          style={{
            opacity: drop,
            transform: [
              { translateY: drop.interpolate({ inputRange: [0, 1], outputRange: [-220, 0] }) },
              { scale: drop.interpolate({ inputRange: [0, 1], outputRange: [0.6, 1] }) },
            ],
          }}
        >
          <Logo size={132} variant={dark ? 'dark' : 'light'} />
        </Animated.View>
      </View>
      <Animated.Text
        style={[
          styles.title,
          { color: c.text, opacity: title, transform: [{ translateY: title.interpolate({ inputRange: [0, 1], outputRange: [14, 0] }) }] },
        ]}
      >
        Spot Reminder
      </Animated.Text>
      <Animated.Text style={[styles.tag, { color: c.sub, opacity: title }]}>Never miss a place you love</Animated.Text>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  root: { ...StyleSheet.absoluteFill, alignItems: 'center', justifyContent: 'center', zIndex: 99 },
  center: { width: 160, height: 160, alignItems: 'center', justifyContent: 'center' },
  ring: { position: 'absolute', width: 132, height: 132, borderRadius: 66, borderWidth: 3 },
  title: { marginTop: 36, fontSize: 30, fontWeight: '800', letterSpacing: 0.5 },
  tag: { marginTop: 6, fontSize: 14 },
});
