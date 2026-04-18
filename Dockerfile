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

WORKDIR /opt/app

ARG VERSION=dev
ARG VCS_REF=unknown
ARG BUILD_DATE=unknown

LABEL maintainer="RuiShan <harryrong@ruishanio.com>"

# 设置时区为 Asia/Shanghai
RUN echo "Asia/Shanghai" > /etc/timezone && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    dpkg-reconfigure -f noninteractive tzdata

COPY --from=backend-builder /workspace/task-pilot-admin/target/task-pilot-admin-*.jar /opt/app/app.jar
# 拷贝启动脚本
COPY ./docker-entrypoint.sh /usr/local/bin/
# 添加执行权限
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 8080

# 设置数据卷
VOLUME /opt/config/

ENTRYPOINT ["docker-entrypoint.sh"]
