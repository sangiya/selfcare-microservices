# QA Automation — Mobile (Detox)

## Why Detox, not Playwright

Playwright automates real **browsers** (Chromium/Firefox/WebKit) — it has no ability to drive a
compiled React Native app on iOS/Android, because there's no browser/DOM for it to attach to.
Detox is built specifically for React Native: it runs against a real simulator/emulator,
understands RN's async rendering (so it waits for the app to go idle instead of guessing with
sleeps), and is maintained by Wix, who also maintain a large share of the RN ecosystem tooling.
Free and open source (MIT license), no paid tier.

If you'd rather stay framework-agnostic across RN/native-iOS/native-Android/Flutter, Appium
(Apache 2.0, also free) is the other common choice — worth considering if the mobile app is
ever anything other than pure React Native.

## Current state: template only

There's no React Native app in this repo yet — this monorepo is the Java backend only. This
folder is a ready-to-use scaffold: config, one placeholder test, npm scripts. Nothing here runs
successfully until a real RN app exists to point it at.

## Activating this once the RN app exists

1. Put the RN app in a sibling directory (`.detoxrc.js` assumes `../../mobile-app/` — adjust
   the paths if it lives elsewhere).
2. `npm install` here, then `npm install detox --save-dev` inside the RN app itself too (Detox
   needs a native module linked into the app).
3. Update the `binaryPath`/`build` commands in `.detoxrc.js` to match the RN app's actual
   Xcode scheme / Gradle module names.
4. Add real `testID` props to the RN components the tests need to find (see
   `e2e/starter.test.js`'s header comment), then set `DETOX_APP_READY=1` to enable it.
5. `npm run build:ios` (or `:android`), then `npm run test:ios` (or `:android`).

## Check the results

- Terminal shows pass/fail as Jest (Detox's test runner) executes.
- `npm run test:ios` / `:android` also writes standard Jest JSON results Jenkins' `junit` step
  can read once wired up (see `../../Jenkinsfile`'s QA Automation stage).

## Requirements to actually run this

Detox needs a real simulator (macOS + Xcode for iOS) or emulator (Android SDK, any OS) —
unlike Playwright's headless browsers, it can't run purely inside a Linux Docker container
without an Android emulator image and hardware/nested virtualization. If your Jenkins agent is
Linux-only, plan on Android-emulator-in-Docker (works, needs `--privileged` or KVM passthrough)
for the Android leg, and either a macOS CI agent or skipping iOS in CI, running it locally/on a
Mac agent instead.
