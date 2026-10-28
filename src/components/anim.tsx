import React, { useEffect, useRef, useState } from 'react';
import { Animated, Easing, Pressable, StyleProp, Text, TextStyle, ViewStyle } from 'react-native';

/** Fades + slides children up; stagger with `index`. */
export function FadeInUp({ index = 0, children, style }: { index?: number; children: React.ReactNode; style?: StyleProp<ViewStyle> }) {
  const v = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    Animated.timing(v, { toValue: 1, duration: 420, delay: Math.min(index, 8) * 70, easing: Easing.out(Easing.cubic), useNativeDriver: true }).start();
  }, []);
  return (
    <Animated.View style={[style, { opacity: v, transform: [{ translateY: v.interpolate({ inputRange: [0, 1], outputRange: [22, 0] }) }] }]}>
      {children}
    </Animated.View>
  );
}

/** Pressable that springs down slightly while touched. */
export function PressScale({ onPress, children, style }: { onPress?: () => void; children: React.ReactNode; style?: StyleProp<ViewStyle> }) {
  const s = useRef(new Animated.Value(1)).current;
  const to = (n: number) => Animated.spring(s, { toValue: n, friction: 6, tension: 200, useNativeDriver: true }).start();
  return (
    <Pressable onPress={onPress} onPressIn={() => to(0.97)} onPressOut={() => to(1)}>
      <Animated.View style={[style, { transform: [{ scale: s }] }]}>{children}</Animated.View>
    </Pressable>
  );
}

export function CountUp({ value, format, style }: { value: number; format: (n: number) => string; style?: StyleProp<TextStyle> }) {
  const v = useRef(new Animated.Value(0)).current;
  const [text, setText] = useState(format(0));
  useEffect(() => {
    const id = v.addListener(({ value: n }) => setText(format(n)));
    Animated.timing(v, { toValue: value, duration: 900, easing: Easing.out(Easing.cubic), useNativeDriver: false }).start();
    return () => v.removeListener(id);
  }, [value]);
  return <Text style={style}>{text}</Text>;
}

/** Gentle endless up/down bobbing. */
export function Float({ children, amount = 8, duration = 1800, style }: { children: React.ReactNode; amount?: number; duration?: number; style?: StyleProp<ViewStyle> }) {
  const v = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(v, { toValue: 1, duration, easing: Easing.inOut(Easing.sin), useNativeDriver: true }),
        Animated.timing(v, { toValue: 0, duration, easing: Easing.inOut(Easing.sin), useNativeDriver: true }),
      ]),
    );
    loop.start();
    return () => loop.stop();
  }, []);
  return <Animated.View style={[style, { transform: [{ translateY: v.interpolate({ inputRange: [0, 1], outputRange: [amount, -amount] }) }] }]}>{children}</Animated.View>;
}

/** Pulsing dot, used for "recording". */
export function PulseDot({ color }: { color: string }) {
  const v = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const loop = Animated.loop(Animated.timing(v, { toValue: 1, duration: 1100, useNativeDriver: true }));
    loop.start();
    return () => loop.stop();
  }, []);
  return (
    <Animated.View style={{ width: 14, height: 14, alignItems: 'center', justifyContent: 'center' }}>
      <Animated.View
        style={{
          position: 'absolute', width: 14, height: 14, borderRadius: 7, backgroundColor: color,
          opacity: v.interpolate({ inputRange: [0, 1], outputRange: [0.6, 0] }),
          transform: [{ scale: v.interpolate({ inputRange: [0, 1], outputRange: [1, 2.6] }) }],
        }}
      />
      <Animated.View style={{ width: 9, height: 9, borderRadius: 5, backgroundColor: color }} />
    </Animated.View>
  );
}
