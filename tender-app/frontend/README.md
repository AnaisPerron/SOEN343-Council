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

## Restaurant demo accounts

Choose **Restaurant** on the sign-in screen. Both accounts use password `DemoPass123!`. They work only when the app cannot reach the backend. When the backend responds, sign-in uses its result and these mock credentials are not accepted unless matching accounts exist there.

| Email | Restaurant | Starting menu |
| --- | --- | --- |
| `greenbowl@demo.test` | Green Bowl Kitchen | Three menu items ready to edit |
| `littleitaly@demo.test` | Little Italy Pizza | No menu; use the create-menu prompt |

Changes to each demo menu are saved separately. On web, they are stored in that browser's local storage. In Expo Go, they are kept for the current app session.

The **Orders** dashboard link opens the local restaurant queue. Green Bowl Kitchen has sample orders across all nine statuses; Little Italy Pizza starts with no orders. Filter by status, open an order for customer and delivery details, accept or reject placed orders with a reason, start preparation, mark prepared orders ready, or select several orders to delete from the local queue. These actions are frontend-only and do not change backend orders.



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
