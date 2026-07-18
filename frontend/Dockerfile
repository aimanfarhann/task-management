# syntax=docker/dockerfile:1

# --- Build stage: produce the optimized static bundle ---
FROM node:24-alpine AS build
WORKDIR /build

# Install dependencies in a layer keyed on the lockfile so source-only changes
# reuse the cached install. `npm ci` is reproducible from package-lock.json.
COPY package.json package-lock.json ./
RUN npm ci

# Production build (angular.json defaultConfiguration=production) -> dist/taskflow/browser.
COPY . .
RUN npm run build

# --- Runtime stage: serve the SPA via nginx, proxying /api to the backend ---
FROM nginx:alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/taskflow/browser /usr/share/nginx/html
EXPOSE 80
