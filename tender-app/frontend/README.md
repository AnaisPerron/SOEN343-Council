# Tender Mobile App

Minimal Expo app for Tender, built with React Native and Expo Router.

## Incoming orders

The home route opens the restaurant order dashboard and automatically loads the
arrival queue for the active demo restaurant (`restaurantId=1`). The restaurant
ID can be overridden with `EXPO_PUBLIC_RESTAURANT_ID` or a
`/incoming-orders?restaurantId=<id>` route parameter. Choose the Arriving,
Preparing, or Awaiting pickup queue. Awaiting pickup shows orders in `READY`
status. The screen displays arrival time, status, each order line's
quantity and unit price, and the calculated order total. Incoming orders can be
accepted, rejected with a reason, or cancelled. Accepted orders appear in
Preparing with their acceptance time and can be started; preparing orders can
then be marked ready. Incoming and preparation queues are ordered by received
and accepted time, respectively. Actions are submitted immediately and display
an in-page success message when complete.

The backend defaults to an in-memory demo database with sample orders. Start it
from `tender-app/backend` with `./mvnw spring-boot:run`. The API defaults to
`http://localhost:8080`; override it with `EXPO_PUBLIC_API_URL` when the app
runs on a device or emulator that cannot reach the host via `localhost`. The
screen uses the restaurant order queue and lifecycle endpoints under
`/api/restaurants/{restaurantId}/orders`.

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

```text
frontend/
├── assets/                         # App images and branding
├── src/
│   ├── app/                         # Expo Router routes and layouts
│   │   ├── _layout.tsx              # Root navigation stack and safe area provider
│   │   ├── index.tsx                # App entry route
│   │   └── customer/
│   │       ├── components/
│   │       │   ├── RestaurantCard.tsx
│   │       │   ├── RestaurantCard.styles.ts
│   │       │   ├── RestaurantSection.tsx
│   │       │   └── RestaurantSection.styles.ts
│   │       ├── home.tsx             # Customer home screen
│   │       ├── home.styles.ts       # Customer home styles
│   │       └── restaurantSearch.ts  # Restaurant name search helper
│   ├── mocks/                       # Demo data and menu photos
│   │   └── menu-items-imgs/
│   ├── tests/                       # Node tests kept outside Expo route discovery
│   │   └── customer/
│   │       └── restaurantSearch.test.ts
│   └── types/
│       └── restaurant.ts            # Shared restaurant model and status type
├── app.json                         # Expo configuration
├── package.json                     # Dependencies and scripts
└── tsconfig.json                    # TypeScript configuration
```
