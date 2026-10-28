import { Ionicons } from '@expo/vector-icons';
import * as Google from 'expo-auth-session/providers/google';
import * as WebBrowser from 'expo-web-browser';
import React, { useEffect, useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { FadeInUp, Float } from '../components/anim';
import { Logo } from '../components/Logo';
import { Button, Field, Screen } from '../components/ui';
import { googleConfig, googleEnabled, loginLocal, loginWithGoogleToken, registerLocal } from '../services/auth';
import { useStore } from '../store';
import { useTheme } from '../theme';

WebBrowser.maybeCompleteAuthSession();

function GoogleButton({ onUser }: { onUser: (token: string) => Promise<void> }) {
  const [request, response, prompt] = Google.useAuthRequest({
    androidClientId: googleConfig.androidClientId,
    webClientId: googleConfig.webClientId,
  });
  useEffect(() => {
    if (response?.type === 'success') onUser(response.authentication!.accessToken).catch((e) => Alert.alert('Google sign-in', e.message));
  }, [response]);
  return <Button title="Continue with Google" icon="logo-google" kind="ghost" disabled={!request} onPress={() => prompt()} />;
}

export function AuthScreen() {
  const { c, dark } = useTheme();
  const signIn = useStore((s) => s.signIn);
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    if (!/^\S+@\S+\.\S+$/.test(email)) return Alert.alert('Invalid email', 'Enter a valid email address.');
    if (password.length < 6) return Alert.alert('Weak password', 'Use at least 6 characters.');
    if (mode === 'register' && !name.trim()) return Alert.alert('Name required', 'Tell us what to call you.');
    setBusy(true);
    try {
      const user = mode === 'register' ? await registerLocal(name, email, password) : await loginLocal(email, password);
      await signIn(user);
    } catch (e: any) {
      Alert.alert(mode === 'register' ? 'Could not sign up' : 'Could not sign in', e.message);
      setBusy(false);
    }
  };

  const google = async (token: string) => signIn(await loginWithGoogleToken(token));

  return (
    <Screen>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={{ padding: 24, flexGrow: 1, justifyContent: 'center' }} keyboardShouldPersistTaps="handled">
          <LinearGradient
            colors={dark ? ['#0F766E', '#0B3A5B'] : ['#14B8A6', '#0369A1']}
            start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}
            style={{ alignItems: 'center', borderRadius: 28, paddingVertical: 28, marginBottom: 24, overflow: 'hidden' }}
          >
            <View style={{ position: 'absolute', right: -40, top: -40, width: 150, height: 150, borderRadius: 75, backgroundColor: '#FFFFFF1A' }} />
            <View style={{ position: 'absolute', left: -30, bottom: -50, width: 120, height: 120, borderRadius: 60, backgroundColor: '#FFFFFF14' }} />
            <Float style={{ position: 'absolute', left: 24, top: 18 }} amount={5}><Text style={{ fontSize: 26 }}>🏖️</Text></Float>
            <Float style={{ position: 'absolute', right: 26, top: 40 }} amount={7} duration={2300}><Text style={{ fontSize: 24 }}>✈️</Text></Float>
            <Float style={{ position: 'absolute', right: 40, bottom: 20 }} amount={4} duration={2000}><Text style={{ fontSize: 22 }}>⛰️</Text></Float>
            <Logo size={92} variant={dark ? 'dark' : 'light'} />
            <Text style={{ color: '#fff', fontSize: 28, fontWeight: '800', marginTop: 12 }}>Spot Reminder</Text>
            <Text style={{ color: '#FFFFFFD9', marginTop: 4 }}>
              {mode === 'login' ? 'Welcome back, traveller' : 'Create your travel account'}
            </Text>
          </LinearGradient>
          <FadeInUp>
          {mode === 'register' && <Field label="Name" value={name} onChangeText={setName} placeholder="Your name" />}
          <Field label="Email" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" placeholder="you@gmail.com" />
          <Field label="Password" value={password} onChangeText={setPassword} secureTextEntry placeholder="At least 6 characters" />
          <Button title={mode === 'login' ? 'Sign in' : 'Create account'} onPress={submit} loading={busy} style={{ marginTop: 4 }} />

          <View style={{ flexDirection: 'row', alignItems: 'center', marginVertical: 18 }}>
            <View style={{ flex: 1, height: 1, backgroundColor: c.border }} />
            <Text style={{ color: c.sub, marginHorizontal: 10 }}>or</Text>
            <View style={{ flex: 1, height: 1, backgroundColor: c.border }} />
          </View>

          {googleEnabled ? (
            <GoogleButton onUser={google} />
          ) : (
            <Button
              title="Continue with Google" icon="logo-google" kind="ghost"
              onPress={() => Alert.alert('Google sign-in not configured', 'Set EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID when building. See README → "Google sign-in".')}
            />
          )}

          </FadeInUp>
          <Text onPress={() => setMode(mode === 'login' ? 'register' : 'login')} style={{ color: c.primary, textAlign: 'center', marginTop: 22, fontWeight: '700' }}>
            {mode === 'login' ? "New here? Create an account" : 'Already have an account? Sign in'}
          </Text>
          <View style={{ flexDirection: 'row', justifyContent: 'center', marginTop: 18 }}>
            <Ionicons name="map" size={14} color={c.sub} />
            <Text style={{ color: c.sub, fontSize: 12, marginLeft: 6 }}>Maps by OpenStreetMap contributors</Text>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
