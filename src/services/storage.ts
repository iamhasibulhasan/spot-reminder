import AsyncStorage from '@react-native-async-storage/async-storage';

export async function getJSON<T>(key: string, fallback: T): Promise<T> {
  try {
    const raw = await AsyncStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

export const setJSON = (key: string, value: unknown) => AsyncStorage.setItem(key, JSON.stringify(value));
export const removeKey = (key: string) => AsyncStorage.removeItem(key);

export const keys = {
  session: 'session',
  users: 'users',
  settings: 'settings',
  activeTrack: 'activeTrack',
  spots: (uid: string) => `spots:${uid}`,
  tracks: (uid: string) => `tracks:${uid}`,
};
