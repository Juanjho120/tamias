# syntax=docker/dockerfile:1

FROM node:22-alpine AS build

WORKDIR /app

ARG FRONTEND_API_BASE_URL=/api/v1
ENV FRONTEND_API_BASE_URL=${FRONTEND_API_BASE_URL}

COPY package*.json ./
RUN npm ci

COPY . .

RUN npm run build:prod

FROM nginx:1.27-alpine

COPY nginx/templates/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/tamias-frontend/browser /usr/share/nginx/html

EXPOSE 80
