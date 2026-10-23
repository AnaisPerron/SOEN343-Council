# Tender Mobile App

Minimal Expo app for Tender, built with React Native and Expo Router.

## Run

Install dependencies:

```bash
npm ci
```

## Test

Run the frontend unit tests from this directory:

```bash
npm test
```

The tests use Node's built-in test runner and TypeScript type stripping. Use Node 24.11.1, the version pinned in `.nvmrc`. The `engines` field in `package.json` also allows Node `^22.13.0`, `^24.3.0`, `^26.0.0`, or `>=27.0.0`; Node 25 is excluded.

Start the app (from phone):

```bash
npx expo start 
```
Press `w` to open the web app or scan the QR code with Expo Go on iOS or Android.

Alternative: 

Start the app (from the web):

```bash
npx expo start --web
```

## Little Italy Pizza demo

Choose **Restaurant** on the sign-in screen and use either `littleitaly@demo.test` or `littleitaly.manager@demo.test` with password `DemoPass123!`. These are frontend-only demo accounts; they do not require a backend restaurant record.

The first visit to **Menu** shows a create-menu prompt. Add a menu name and items, then save. On web, the demo menu is saved in that browser's local storage. In Expo Go, it is kept for the current app session. Both demo accounts share the same local menu.



## Troubleshooting: Expo Go version mismatch

Expo Go must support the Expo SDK used by the project. This app uses SDK 57. Check the `expo` version in `package.json`; its major version should match the SDK version required by Expo Go.

If Expo Go reports that the project is incompatible:

1. Install the Expo Go build for SDK 57 on a supported device or simulator.
2. Align project dependencies with the SDK:

   ```bash
   npx expo install --fix
   npx expo-doctor
   ```

3. Restart the development server with `npx expo start` and reopen the project.

The iOS App Store version of Expo Go does not support SDK 57. For a physical iPhone, use an SDK 57 development build or an Expo Go build distributed through TestFlight. See [Expo's version mismatch guide](https://docs.expo.dev/troubleshooting/expo-go-version-mismatch/) for platform-specific options.

## Project structure

Customer code is grouped under `src/app/customer/`:

```text
customer/
├── components/       # Shared customer UI
├── customize/        # Item customization screen and styles
├── menu-item/        # Menu item screen and styles
├── restaurant/       # Restaurant detail screen and styles
├── cart/             # Cart screen, styles, and cartStore
├── checkout/         # Confirm order screen and styles
├── home/             # Customer home screen and styles
├── services/         # Menu, search, order, and restriction helpers
├── _layout.tsx       # Customer navigation
└── index.tsx         # Customer entry redirect
```

The root `src/app/_layout.tsx` and `src/app/index.tsx` remain the app entry points for customer and auth routes. Shared demo data is in `src/mocks/`, state providers are in `src/context/`, and tests are in `src/tests/`.
