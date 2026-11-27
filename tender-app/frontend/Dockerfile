FROM node:24-bookworm-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG EXPO_PUBLIC_API_URL=http://localhost:8080
ARG EXPO_PUBLIC_GOOGLE_MAPS_API_KEY
ARG EXPO_PUBLIC_GOOGLE_MAPS_MAP_ID
ENV EXPO_NO_DOTENV=1 CI=1 EXPO_NO_TELEMETRY=1
# EXPO_PUBLIC_API_URL=/ makes the browser call the page's own address (the demo platform's nginx
# proxies /api). Without a Maps key the app still runs, with the Google Maps features off.
RUN test -n "$EXPO_PUBLIC_API_URL" \
    || (echo "Set the EXPO_PUBLIC_API_URL build argument" >&2; exit 1)
RUN test -n "$EXPO_PUBLIC_GOOGLE_MAPS_API_KEY" \
    || echo "warning: EXPO_PUBLIC_GOOGLE_MAPS_API_KEY is empty, Google Maps features are off" >&2
RUN npx expo export --platform web

FROM node:24-bookworm-slim
WORKDIR /app
ENV NODE_ENV=production EXPO_NO_DOTENV=1 CI=1 EXPO_NO_TELEMETRY=1
COPY --from=build --chown=node:node /app/package.json /app/package-lock.json /app/app.json ./
COPY --from=build --chown=node:node /app/node_modules ./node_modules
COPY --from=build --chown=node:node /app/dist ./dist
USER node
EXPOSE 8081
CMD ["./node_modules/.bin/expo", "serve", "dist", "--port", "8081"]
