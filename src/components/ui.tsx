import { Ionicons } from '@expo/vector-icons';
import React from 'react';
import {
  ActivityIndicator, Pressable, StyleProp, StyleSheet, Text, TextInput, TextInputProps, View, ViewStyle,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTheme } from '../theme';

type Icon = React.ComponentProps<typeof Ionicons>['name'];

export function Screen({ children, edges = ['top'] }: { children: React.ReactNode; edges?: ('top' | 'bottom')[] }) {
  const { c } = useTheme();
  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: c.bg }} edges={edges}>
      {children}
    </SafeAreaView>
  );
}

export function Card({ children, style }: { children: React.ReactNode; style?: StyleProp<ViewStyle> }) {
  const { c } = useTheme();
  return <View style={[styles.card, { backgroundColor: c.card, borderColor: c.border }, style]}>{children}</View>;
}

export function Button({
  title, onPress, icon, kind = 'primary', loading, disabled, style,
}: {
  title: string; onPress: () => void; icon?: Icon; kind?: 'primary' | 'ghost' | 'danger'; loading?: boolean;
  disabled?: boolean; style?: StyleProp<ViewStyle>;
}) {
  const { c } = useTheme();
  const bg = kind === 'primary' ? c.primary : kind === 'danger' ? c.danger : 'transparent';
  const fg = kind === 'primary' ? c.onPrimary : kind === 'danger' ? '#fff' : c.primary;
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      style={({ pressed }) => [
        styles.btn,
        { backgroundColor: bg, borderColor: kind === 'ghost' ? c.primary : bg, opacity: disabled ? 0.5 : pressed ? 0.85 : 1 },
        style,
      ]}
    >
      {loading ? <ActivityIndicator color={fg} /> : (
        <>
          {icon && <Ionicons name={icon} size={18} color={fg} style={{ marginRight: 8 }} />}
          <Text style={{ color: fg, fontWeight: '700', fontSize: 15 }}>{title}</Text>
        </>
      )}
    </Pressable>
  );
}

export function Field({ label, ...props }: TextInputProps & { label: string }) {
  const { c } = useTheme();
  return (
    <View style={{ marginBottom: 12 }}>
      <Text style={[styles.label, { color: c.sub }]}>{label}</Text>
      <TextInput
        placeholderTextColor={c.sub}
        {...props}
        style={[styles.input, { backgroundColor: c.input, color: c.text, borderColor: c.border }, props.multiline && { height: 80, textAlignVertical: 'top' }]}
      />
    </View>
  );
}

export function Chip({ label, active, onPress, icon, color }: { label: string; active?: boolean; onPress?: () => void; icon?: Icon; color?: string }) {
  const { c } = useTheme();
  const tint = color ?? c.primary;
  return (
    <Pressable
      onPress={onPress}
      style={[styles.chip, { borderColor: active ? tint : c.border, backgroundColor: active ? tint + '22' : c.card }]}
    >
      {icon && <Ionicons name={icon} size={14} color={active ? tint : c.sub} style={{ marginRight: 4 }} />}
      <Text style={{ color: active ? tint : c.sub, fontWeight: '600', fontSize: 13 }}>{label}</Text>
    </Pressable>
  );
}

export function Header({ title, right, onBack }: { title: string; right?: React.ReactNode; onBack?: () => void }) {
  const { c } = useTheme();
  return (
    <View style={styles.header}>
      {onBack && (
        <Pressable onPress={onBack} hitSlop={10} style={{ marginRight: 10 }}>
          <Ionicons name="chevron-back" size={26} color={c.text} />
        </Pressable>
      )}
      <Text style={[styles.title, { color: c.text }]} numberOfLines={1}>{title}</Text>
      {right}
    </View>
  );
}

export function IconButton({ icon, onPress, color, size = 22 }: { icon: Icon; onPress: () => void; color?: string; size?: number }) {
  const { c } = useTheme();
  return (
    <Pressable onPress={onPress} hitSlop={10} style={{ padding: 6 }}>
      <Ionicons name={icon} size={size} color={color ?? c.text} />
    </Pressable>
  );
}

export function Empty({ icon, text }: { icon: Icon; text: string }) {
  const { c } = useTheme();
  return (
    <View style={{ alignItems: 'center', padding: 40 }}>
      <Ionicons name={icon} size={56} color={c.border} />
      <Text style={{ color: c.sub, marginTop: 10, textAlign: 'center' }}>{text}</Text>
    </View>
  );
}

export function Stat({ label, value, color }: { label: string; value: string; color?: string }) {
  const { c } = useTheme();
  return (
    <View style={{ flex: 1 }}>
      <Text style={{ color: c.sub, fontSize: 12 }}>{label}</Text>
      <Text style={{ color: color ?? c.text, fontSize: 17, fontWeight: '800', marginTop: 2 }}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card: { borderRadius: 16, borderWidth: StyleSheet.hairlineWidth, padding: 14 },
  btn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', borderRadius: 12, borderWidth: 1.5, paddingVertical: 13, paddingHorizontal: 18 },
  label: { fontSize: 12, fontWeight: '600', marginBottom: 5 },
  input: { borderWidth: 1, borderRadius: 12, paddingHorizontal: 14, paddingVertical: 11, fontSize: 15 },
  chip: { flexDirection: 'row', alignItems: 'center', borderWidth: 1.5, borderRadius: 20, paddingHorizontal: 12, paddingVertical: 7, marginRight: 8, marginBottom: 8 },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 12 },
  title: { flex: 1, fontSize: 24, fontWeight: '800' },
});
