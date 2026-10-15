FROM node:20-alpine AS build

WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL=/api
ARG VITE_LOAN_API_URL=/api
ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_LOAN_API_URL=${VITE_LOAN_API_URL}
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O - http://localhost:8080/health || exit 1
