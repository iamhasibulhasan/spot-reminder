// Adds the Google OAuth redirect scheme when a Google *Android* client id is provided at build time.
module.exports = ({ config }) => {
  const id = process.env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID;
  if (!id) return config;
  const reversed = `com.googleusercontent.apps.${id.replace('.apps.googleusercontent.com', '')}`;
  return { ...config, scheme: [config.scheme, reversed] };
};
