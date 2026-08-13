/**
 * TEMPLATE -- there is no React Native app in this repo yet (this monorepo is the Java
 * backend only). The binaryPath/build values below are placeholders that assume the RN app
 * lives in a sibling directory called `mobile-app/`; adjust every path once that app exists.
 * See README.md for the activation steps.
 */
module.exports = {
  testRunner: {
    args: {
      $0: 'jest',
      config: 'e2e/jest.config.js',
    },
    jest: { setupTimeout: 120000 },
  },
  apps: {
    'ios.debug': {
      type: 'ios.app',
      binaryPath: '../../mobile-app/ios/build/Build/Products/Debug-iphonesimulator/MobileApp.app',
      build:
        'cd ../../mobile-app && xcodebuild -workspace ios/MobileApp.xcworkspace ' +
        '-scheme MobileApp -configuration Debug -sdk iphonesimulator -derivedDataPath ios/build',
    },
    'android.debug': {
      type: 'android.apk',
      binaryPath: '../../mobile-app/android/app/build/outputs/apk/debug/app-debug.apk',
      build:
        'cd ../../mobile-app/android && ./gradlew assembleDebug assembleAndroidTest ' +
        '-DtestBuildType=debug',
    },
  },
  devices: {
    simulator: {
      type: 'ios.simulator',
      device: { type: 'iPhone 15' },
    },
    emulator: {
      type: 'android.emulator',
      device: { avdName: 'Pixel_7_API_34' },
    },
  },
  configurations: {
    'ios.sim.debug': { device: 'simulator', app: 'ios.debug' },
    'android.emu.debug': { device: 'emulator', app: 'android.debug' },
  },
};
