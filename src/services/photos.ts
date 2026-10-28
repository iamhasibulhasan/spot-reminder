import { File, Paths } from 'expo-file-system';
import * as ImagePicker from 'expo-image-picker';

/** Lets the user choose an image and copies it into app storage so it survives cache cleanup. */
export async function pickPhoto(aspect: [number, number] = [1, 1]): Promise<string | null> {
  const r = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], allowsEditing: true, aspect, quality: 0.6 });
  if (r.canceled) return null;
  const src = r.assets[0].uri;
  try {
    const dest = new File(Paths.document, `img-${Date.now()}.jpg`);
    new File(src).copy(dest);
    return dest.uri;
  } catch {
    return src;
  }
}
