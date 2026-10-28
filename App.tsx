import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import React, { useEffect, useState } from 'react';
import { View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AnimatedSplash } from './src/components/AnimatedSplash';
import { Navigation } from './src/navigation';
import { setupNotifications } from './src/services/tasks';
import { useStore } from './src/store';
import { useTheme } from './src/theme';

SplashScreen.preventAutoHideAsync().catch(() => {});

export default function App() {
  const init = useStore((s) => s.init);
  const ready = useStore((s) => s.ready);
  const { c, dark } = useTheme();
  const [splash, setSplash] = useState(true);

  useEffect(() => {
    init().finally(() => SplashScreen.hideAsync().catch(() => {}));
    setupNotifications().catch(() => {});
  }, []);

  return (
    <SafeAreaProvider>
      <View style={{ flex: 1, backgroundColor: c.bg }}>
        <StatusBar style={dark ? 'light' : 'dark'} />
        {ready && <Navigation />}
        {splash && <AnimatedSplash dark={dark} onDone={() => setSplash(false)} />}
      </View>
    </SafeAreaProvider>
  );
}
