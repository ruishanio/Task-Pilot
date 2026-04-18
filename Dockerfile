# 先构建前端静态资源，确保运行镜像不携带 Node 环境。
FROM node:20-alpine AS frontend-builder
WORKDIR /workspace/task-pilot-frontend

COPY task-pilot-frontend/package.json task-pilot-frontend/package-lock.json ./
RUN npm ci

COPY task-pilot-frontend/ ./
RUN npm run build

# 服务端阶段把前端产物回填到 Admin 静态目录，再统一打包 Spring Boot Jar。
FROM maven:3.9.9-eclipse-temurin-21 AS backend-builder
WORKDIR /workspace

COPY . .
COPY --from=frontend-builder /workspace/task-pilot-frontend/dist ./task-pilot-frontend/dist

RUN rm -rf task-pilot-admin/src/main/resources/static \
    && mkdir -p task-pilot-admin/src/main/resources/static \
    && cp -r task-pilot-frontend/dist/. task-pilot-admin/src/main/resources/static/ \
    && mvn -B -ntp -P '!release' -pl task-pilot-admin -am package -DskipTests

# 运行时阶段仅保留最终 Jar 和启动参数，尽量收窄镜像体积。
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

ARG VERSION=dev
ARG VCS_REF=unknown
ARG BUILD_DATE=unknown

LABEL org.opencontainers.image.title="task-pilot-admin" \
      org.opencontainers.image.description="Task-Pilot admin server image" \
      org.opencontainers.image.url="https://hub.docker.com/r/ruishan/task-pilot" \
      org.opencontainers.image.source="https://github.com/ruishanio/Task-Pilot" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.created="${BUILD_DATE}"

ENV PARAMS=""
ENV JAVA_OPTS=""
ENV TZ=Asia/Shanghai

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=backend-builder /workspace/task-pilot-admin/target/task-pilot-admin-*.jar /app/app.jar

EXPOSE 8080

# LOG_HOME、JAVA_OPTS、PARAMS 都通过环境变量注入，便于沿用现有部署方式。
ENTRYPOINT ["sh","-c","java ${LOG_HOME:+-DLOG_HOME=$LOG_HOME} $JAVA_OPTS -jar /app/app.jar $PARAMS"]
