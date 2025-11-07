# ===== build stage =====
FROM node:22-alpine AS build
WORKDIR /app
ENV CI=true
ARG VITE_KAKAO_REST_API_KEY
ARG VITE_KAKAO_JS_API_KEY
ARG VITE_APP_DEVELOP_FRONT_URL
ARG VITE_APP_DEVELOP_BACKEND_URL

ENV VITE_KAKAO_REST_API_KEY=$VITE_KAKAO_REST_API_KEY \
    VITE_KAKAO_JS_API_KEY=$VITE_KAKAO_JS_API_KEY \
    VITE_APP_DEVELOP_FRONT_URL=$VITE_APP_DEVELOP_FRONT_URL \
    VITE_APP_DEVELOP_BACKEND_URL=$VITE_APP_DEVELOP_BACKEND_URL

COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

# ===== run stage =====
FROM nginx:alpine
WORKDIR /usr/share/nginx/html
COPY --from=build /app/dist ./
COPY ./conf/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
