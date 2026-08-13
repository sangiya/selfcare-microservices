/**
 * Detox drives the compiled React Native app, not a browser -- which is why Playwright can't
 * cover mobile and this exists separately.
 *
 * Nothing here runs until the React Native app is available, because Detox needs a built binary:
 * set RN_APP_ROOT to the checkout of the mobile app and, if your build output differs from the
 * React Native defaults below, DETOX_IOS_APP_PATH / DETOX_ANDROID_APP_PATH too. In CI the mobile
 * repo would normally own this file; it lives here so the pipeline stage and the conventions
 * (testID naming, Allure output) are agreed before that repo exists.
 *
 * @type {Detox.DetoxConfig}
 */
const path = require('path');

const appRoot = process.env.RN_APP_ROOT ?? '../../../selfcare-mobile';

const iosBinary =
  process.env.DETOX_IOS_APP_PATH ??
  path.join(appRoot, 'ios/build/Build/Products/Debug-iphonesimulator/selfcare.app');

const androidBinary =
  process.env.DETOX_ANDROID_APP_PATH ??
  path.join(appRoot, 'android/app/build/outputs/apk/debug/app-debug.apk');

module.exports = {
  testRunner: {
    args: {
      $0: 'jest',
      config: 'e2e/jest.config.js',
    },
    jest: {
      setupTimeout: 120000,
    },
  },
  apps: {
    'ios.debug': {
      type: 'ios.app',
      binaryPath: iosBinary,
      build: `cd ${appRoot}/ios && xcodebuild -workspace selfcare.xcworkspace -scheme selfcare -configuration Debug -sdk iphonesimulator -derivedDataPath ./build`,
    },
    'android.debug': {
      type: 'android.apk',
      binaryPath: androidBinary,
      build: `cd ${appRoot}/android && ./gradlew assembleDebug assembleAndroidTest -DtestBuildType=debug`,
    },
  },
  devices: {
    simulator: {
      type: 'ios.simulator',
      device: { type: process.env.DETOX_IOS_DEVICE ?? 'iPhone 15' },
    },
    emulator: {
      type: 'android.emulator',
      device: { avdName: process.env.DETOX_ANDROID_AVD ?? 'Pixel_7_API_34' },
    },
  },
  configurations: {
    'ios.sim.debug': { device: 'simulator', app: 'ios.debug' },
    'android.emu.debug': { device: 'emulator', app: 'android.debug' },
  },
};
