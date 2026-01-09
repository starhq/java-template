# 使用官方 Eclipse Temurin 镜像（OpenJDK）
FROM eclipse-temurin:21-jre-alpine

# 设置工作目录
WORKDIR /app

# 复制 JAR 文件（Gradle 默认输出路径）
COPY build/libs/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]