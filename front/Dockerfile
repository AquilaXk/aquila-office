# syntax=docker/dockerfile:1.7

FROM node:24-alpine AS dependencies

WORKDIR /workspace/front

COPY front/package.json front/package-lock.json ./

RUN npm ci

FROM node:24-alpine AS builder

WORKDIR /workspace/front

ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
ARG NEXT_PUBLIC_GOOGLE_CLIENT_ID=
ARG NEXT_PUBLIC_SITE_URL=http://localhost:3000
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
ENV NEXT_PUBLIC_GOOGLE_CLIENT_ID=${NEXT_PUBLIC_GOOGLE_CLIENT_ID}
ENV NEXT_PUBLIC_SITE_URL=${NEXT_PUBLIC_SITE_URL}
ENV NEXT_TELEMETRY_DISABLED=1

COPY --from=dependencies /workspace/front/node_modules ./node_modules
COPY front/ ./

RUN npm run build

FROM node:24-alpine AS runtime

ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
ARG NEXT_PUBLIC_GOOGLE_CLIENT_ID=
ARG NEXT_PUBLIC_SITE_URL=http://localhost:3000
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV HOSTNAME=0.0.0.0
ENV PORT=3000
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
ENV NEXT_PUBLIC_GOOGLE_CLIENT_ID=${NEXT_PUBLIC_GOOGLE_CLIENT_ID}
ENV NEXT_PUBLIC_SITE_URL=${NEXT_PUBLIC_SITE_URL}

WORKDIR /app

COPY front/package.json front/package-lock.json ./

RUN npm ci --omit=dev --ignore-scripts && npm cache clean --force

COPY --from=builder /workspace/front/.next ./.next
COPY --from=builder /workspace/front/public ./public
COPY --from=builder /workspace/front/next.config.ts ./next.config.ts

USER node

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://127.0.0.1:3000/ >/dev/null || exit 1

CMD ["npm", "run", "start"]
