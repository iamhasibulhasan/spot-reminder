import { useColorScheme } from 'react-native';
import { useStore } from '../store';

export const palettes = {
  light: {
    bg: '#F4F7F8', card: '#FFFFFF', text: '#0F172A', sub: '#64748B', border: '#E2E8F0',
    primary: '#0D9488', onPrimary: '#FFFFFF', accent: '#F97316', danger: '#DC2626', input: '#F1F5F9',
  },
  dark: {
    bg: '#0B1220', card: '#131C2E', text: '#F1F5F9', sub: '#94A3B8', border: '#233047',
    primary: '#2DD4BF', onPrimary: '#042F2E', accent: '#FB923C', danger: '#F87171', input: '#1A2540',
  },
};
export type Palette = typeof palettes.light;

export function useTheme() {
  const system = useColorScheme();
  const pref = useStore((s) => s.settings.theme);
  const mode = pref === 'system' ? (system === 'dark' ? 'dark' : 'light') : pref;
  return { mode, c: palettes[mode] as Palette, dark: mode === 'dark' };
}
