#!/bin/bash
# Atlas Build Script
# Run this on a machine with Android SDK installed:
#   chmod +x build-apk.sh && ./build-apk.sh

set -e

echo "🔨 Building Atlas APK..."

# Install deps if needed
if [ ! -d "node_modules" ]; then
  echo "📦 Installing dependencies..."
  npm install
fi

# Build web assets
echo "🌐 Building web bundle..."
npx vite build

# Sync with Capacitor
echo "📱 Syncing with Android..."
npx cap sync android

# Build APK
echo "📦 Building APK..."
cd android
./gradlew assembleDebug
cd ..

echo ""
echo "✅ APK built successfully!"
echo "📄 Location: android/app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "To install on your phone:"
echo "  1. Enable USB debugging on your Android device"
echo "  2. Connect via USB"
echo "  3. Run: adb install android/app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "Or copy the APK to your phone and tap to install."
