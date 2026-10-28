import { Ionicons } from '@expo/vector-icons';
import { File, Paths } from 'expo-file-system';
import * as ImagePicker from 'expo-image-picker';
import React, { useState } from 'react';
import { Alert, Image, Pressable, ScrollView, Text, View } from 'react-native';
import { Logo } from '../components/Logo';
import { Button, Card, Chip, Field, Header, Screen } from '../components/ui';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { CURRENCIES } from '../utils/cost';

export function ProfileScreen() {
  const { c, dark } = useTheme();
  const { user, settings, updateUser, setSettings, signOut } = useStore();
  const [name, setName] = useState(user!.name);
  const [phone, setPhone] = useState(user!.phone ?? '');
  const dirty = name.trim() !== user!.name || phone.trim() !== (user!.phone ?? '');

  const pickPhoto = async () => {
    const r = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], allowsEditing: true, aspect: [1, 1], quality: 0.6 });
    if (r.canceled) return;
    try {
      // copy into app storage so the picker's cache cleanup can't remove it
      const dest = new File(Paths.document, `avatar-${user!.id}-${Date.now()}.jpg`);
      new File(r.assets[0].uri).copy(dest);
      await updateUser({ photo: dest.uri });
    } catch {
      await updateUser({ photo: r.assets[0].uri });
    }
  };

  const save = async () => {
    if (!name.trim()) return Alert.alert('Name required', 'Your name cannot be empty.');
    await updateUser({ name: name.trim(), phone: phone.trim() || undefined });
    Alert.alert('Saved', 'Profile updated.');
  };

  return (
    <Screen>
      <Header title="Profile" />
      <ScrollView contentContainerStyle={{ padding: 16, paddingTop: 0, paddingBottom: 40 }} keyboardShouldPersistTaps="handled">
        <View style={{ alignItems: 'center', marginBottom: 18 }}>
          <Pressable onPress={pickPhoto}>
            {user!.photo ? (
              <Image source={{ uri: user!.photo }} style={{ width: 96, height: 96, borderRadius: 48 }} />
            ) : (
              <View style={{ width: 96, height: 96, borderRadius: 48, backgroundColor: c.primary, alignItems: 'center', justifyContent: 'center' }}>
                <Text style={{ color: c.onPrimary, fontSize: 38, fontWeight: '800' }}>{user!.name[0]?.toUpperCase()}</Text>
              </View>
            )}
            <View style={{ position: 'absolute', right: 0, bottom: 0, backgroundColor: c.accent, borderRadius: 16, padding: 6 }}>
              <Ionicons name="camera" size={16} color="#fff" />
            </View>
          </Pressable>
          <Text style={{ color: c.sub, marginTop: 8 }}>{user!.email} · {user!.provider === 'google' ? 'Google' : 'Email'} account</Text>
        </View>

        <Field label="Name" value={name} onChangeText={setName} />
        <Field label="Phone" value={phone} onChangeText={setPhone} keyboardType="phone-pad" placeholder="Optional" />
        <Button title="Save changes" icon="checkmark" onPress={save} disabled={!dirty} />

        <Text style={{ color: c.text, fontWeight: '800', fontSize: 16, marginTop: 24, marginBottom: 8 }}>Currency</Text>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
          {CURRENCIES.map((cu) => (
            <Chip key={cu.code} label={`${cu.symbol.trim()} ${cu.code}`} active={settings.currency === cu.code} onPress={() => setSettings({ currency: cu.code })} />
          ))}
        </View>

        <Text style={{ color: c.text, fontWeight: '800', fontSize: 16, marginTop: 14, marginBottom: 8 }}>Appearance</Text>
        <View style={{ flexDirection: 'row' }}>
          {(['system', 'light', 'dark'] as const).map((m) => (
            <Chip key={m} label={m} active={settings.theme === m} onPress={() => setSettings({ theme: m })} icon={m === 'dark' ? 'moon' : m === 'light' ? 'sunny' : 'phone-portrait'} />
          ))}
        </View>

        <Card style={{ marginTop: 16, flexDirection: 'row', alignItems: 'center' }}>
          <Logo size={44} variant={dark ? 'dark' : 'light'} />
          <View style={{ marginLeft: 12, flex: 1 }}>
            <Text style={{ color: c.text, fontWeight: '700' }}>Spot Reminder</Text>
            <Text style={{ color: c.sub, fontSize: 12 }}>Maps © OpenStreetMap contributors</Text>
          </View>
        </Card>

        <Button title="Sign out" kind="danger" icon="log-out-outline" onPress={() => Alert.alert('Sign out?', '', [
          { text: 'Cancel', style: 'cancel' }, { text: 'Sign out', style: 'destructive', onPress: signOut },
        ])} style={{ marginTop: 20 }} />
      </ScrollView>
    </Screen>
  );
}
