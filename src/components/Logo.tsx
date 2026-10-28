import React from 'react';
import Svg, { Circle, Defs, Ellipse, LinearGradient, Path, Rect, Stop } from 'react-native-svg';

type Props = { size?: number; variant?: 'light' | 'dark'; tile?: boolean };

/** Light: white pin on teal→blue tile. Dark: glowing teal pin on midnight tile. */
export function Logo({ size = 96, variant = 'light', tile = true }: Props) {
  const dark = variant === 'dark';
  const pin = dark ? '#2DD4BF' : '#FFFFFF';
  const shadow = dark ? '#2DD4BF' : '#042F2E';
  return (
    <Svg width={size} height={size} viewBox="0 0 1024 1024">
      <Defs>
        <LinearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor={dark ? '#0F1B33' : '#14B8A6'} />
          <Stop offset="1" stopColor={dark ? '#05080F' : '#0369A1'} />
        </LinearGradient>
      </Defs>
      {tile && <Rect width="1024" height="1024" rx="230" fill="url(#bg)" />}
      <Ellipse cx="512" cy="835" rx="150" ry="36" fill={shadow} opacity={0.35} />
      <Ellipse cx="512" cy="835" rx="100" ry="22" fill={shadow} opacity={0.5} />
      <Path d="M512 160C376 160 268 268 268 402c0 178 244 405 244 405s244-227 244-405C756 268 648 160 512 160z" fill={pin} />
      <Circle cx="512" cy="402" r="104" fill="#F97316" />
      <Path d="M512 330v76l52 32" stroke={pin} strokeWidth={30} strokeLinecap="round" strokeLinejoin="round" fill="none" />
    </Svg>
  );
}
