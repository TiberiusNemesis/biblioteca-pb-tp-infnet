FROM node:20-alpine AS build

WORKDIR /workspace

ENV PNPM_HOME=/pnpm
ENV PATH="$PNPM_HOME/bin:$PATH"
RUN apk add --no-cache curl openssl \
  && touch /root/.profile \
  && curl -fsSL https://get.pnpm.io/install.sh | env PNPM_VERSION=12.8.1 SHELL=/bin/sh ENV=/root/.profile sh -

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
ARG VITE_API_URL=/api
ARG VITE_LOAN_API_URL=/api
ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_LOAN_API_URL=${VITE_LOAN_API_URL}
RUN pnpm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O - http://localhost:8080/health || exit 1
