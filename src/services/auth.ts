import * as Crypto from 'expo-crypto';
import { User } from '../types';
import { getJSON, keys, setJSON } from './storage';

const hash = (salt: string, pw: string) => Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256, salt + pw);
const norm = (e: string) => e.trim().toLowerCase();

export const loadUsers = () => getJSON<User[]>(keys.users, []);

export async function registerLocal(name: string, email: string, password: string): Promise<User> {
  const users = await loadUsers();
  if (users.some((u) => u.email === norm(email))) throw new Error('An account with this email already exists');
  const salt = Crypto.randomUUID();
  const user: User = {
    id: Crypto.randomUUID(), name: name.trim(), email: norm(email), provider: 'local',
    salt, passwordHash: await hash(salt, password),
  };
  await setJSON(keys.users, [...users, user]);
  return user;
}

export async function loginLocal(email: string, password: string): Promise<User> {
  const user = (await loadUsers()).find((u) => u.email === norm(email));
  if (!user) throw new Error('No account found for this email');
  if (user.provider === 'google' && !user.passwordHash) throw new Error('This email uses Google sign-in');
  if ((await hash(user.salt!, password)) !== user.passwordHash) throw new Error('Wrong password');
  return user;
}

export async function loginWithGoogleToken(accessToken: string): Promise<User> {
  const res = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!res.ok) throw new Error('Google sign-in failed');
  const p = await res.json();
  const users = await loadUsers();
  const existing = users.find((u) => u.email === norm(p.email));
  if (existing) return existing;
  const user: User = { id: Crypto.randomUUID(), name: p.name ?? p.email, email: norm(p.email), photo: p.picture, provider: 'google' };
  await setJSON(keys.users, [...users, user]);
  return user;
}

export const googleConfig = {
  androidClientId: process.env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID,
  webClientId: process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID,
};
export const googleEnabled = !!googleConfig.androidClientId;
