FROM node:24-bookworm-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG EXPO_PUBLIC_API_URL=http://localhost:8080
ARG EXPO_PUBLIC_GOOGLE_MAPS_API_KEY
ARG EXPO_PUBLIC_GOOGLE_MAPS_MAP_ID
ENV EXPO_NO_DOTENV=1 CI=1 EXPO_NO_TELEMETRY=1
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
