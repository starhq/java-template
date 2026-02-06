# syntax=docker/dockerfile:1.4

# ========================================
# 阶段1：构建
# ========================================
FROM gradle:9.3.0-jdk25-alpine AS builder
WORKDIR /app

# 拷贝构建配置
COPY gradle/wrapper gradle/wrapper
COPY build.gradle settings.gradle gradlew ./

# 拷贝源码
COPY src src

# ✅ 构建命令
# 1. 恢复了 /app/.gradle 挂载，配合 --build-cache 使用，极大提升增量构建速度
# 2. 使用 bootJar 替代 build (如果不希望在构建镜像时运行测试)
RUN --mount=type=cache,target=/home/gradle/.gradle \
    --mount=type=cache,target=/app/.gradle \
    --mount=type=cache,target=/app/build \
    ./gradlew bootJar --no-daemon --parallel --build-cache && \
    cp build/libs/*.jar app.jar

# ========================================
# 阶段2：运行
# ========================================
FROM eclipse-temurin:25.0.2_10-jre-alpine

# ========================================
# Layer 1: 基础设置与环境 (变化频率低)
# ========================================
# 安装运行时依赖 (curl 用于健康检查, unzip 用于分层加载)
# 使用 --no-cache 减少层数大小
RUN apk add --no-cache curl unzip && \
    # 处理时区 (使用虚拟包技巧，安装完立刻删除 tzdata 数据包，只保留时区文件)
    apk add --no-cache --virtual .build-deps tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del .build-deps

# ========================================
# Layer 2: 用户与目录 (变化频率极低)
# ========================================
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    mkdir -p /app && \
    chown -R appuser:appgroup /app

USER appuser
WORKDIR /app

# ========================================
# Layer 3: 应用文件 (变化频率高)
# ========================================
COPY --from=builder --chown=appuser:appgroup /app/app.jar app.jar

# 提取分层
# 注意：这里不能删除 unzip，因为运行时 JarLauncher 需要它
RUN java -Djarmode=layertools -jar app.jar extract --destination . && \
    rm app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 优化参数
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# 使用分层启动
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp dependencies:spring-boot-loader:snapshot-dependencies:application org.springframework.boot.loader.launch.JarLauncher"]


# DOCKER_BUILDKIT=1 docker build -t sonardemo .