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

The tests use Node's built-in test runner and TypeScript type stripping, so use the Node version in `.nvmrc` (Node 22.13 or newer).

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
├── src/app/       # Expo Router screens and layout
├── app.json       # Expo configuration
├── package.json   # Dependencies and commands
└── tsconfig.json  # TypeScript configuration
```
