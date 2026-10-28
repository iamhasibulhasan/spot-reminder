import { LinearGradient } from 'expo-linear-gradient';
import React from 'react';
import { Image, StyleProp, StyleSheet, Text, View, ViewStyle } from 'react-native';
import { Spot } from '../types';
import { typeMeta } from '../utils/spotTypes';

/** Spot photo, or a themed gradient with a big emoji and soft decorative circles. */
export function SpotCover({
  spot, height, radius = 16, children, style,
}: { spot: Pick<Spot, 'type' | 'photo'>; height: number; radius?: number; children?: React.ReactNode; style?: StyleProp<ViewStyle> }) {
  const t = typeMeta(spot.type);
  return (
    <View style={[{ height, borderRadius: radius, overflow: 'hidden' }, style]}>
      {spot.photo ? (
        <>
          <Image source={{ uri: spot.photo }} style={StyleSheet.absoluteFill} resizeMode="cover" />
          <LinearGradient colors={['#0000', '#000A']} style={StyleSheet.absoluteFill} />
        </>
      ) : (
        <LinearGradient colors={t.colors} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={StyleSheet.absoluteFill}>
          <View style={[styles.circle, { width: height * 1.1, height: height * 1.1, right: -height * 0.3, top: -height * 0.4 }]} />
          <View style={[styles.circle, { width: height * 0.7, height: height * 0.7, left: -height * 0.2, bottom: -height * 0.3 }]} />
          <View style={[StyleSheet.absoluteFill, { alignItems: 'center', justifyContent: 'center' }]}>
            <Text style={{ fontSize: Math.min(height * 0.5, 72) }}>{t.emoji}</Text>
          </View>
        </LinearGradient>
      )}
      {children}
    </View>
  );
}

const styles = StyleSheet.create({ circle: { position: 'absolute', borderRadius: 999, backgroundColor: '#FFFFFF22' } });
