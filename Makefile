# =============================================================================
# Spring Boot 项目 Makefile（Gradle 版）
# 功能：统一开发、测试、构建、Docker 操作
# 作者：Your Name
# 日期：2025-01-09
# =============================================================================

# ==============================
# 配置变量（按需修改）
# ==============================
APP_NAME := my-spring-app
MAIN_CLASS := com.example.MySpringBootApplication
SPRING_PROFILES_ACTIVE := local
SERVER_PORT := 8080

# Gradle 执行器（支持 ./gradlew 或 gradle）
GRADLE := ./gradlew
ifeq (,$(wildcard ./gradlew))
	GRADLE := gradle
endif

# Docker 镜像名
DOCKER_IMAGE := $(APP_NAME):latest

# ==============================
# 核心命令
# ==============================

.PHONY: help
help:
	@echo "Spring Boot 项目 Makefile 帮助 (Gradle 版)"
	@echo "----------------------------------------"
	@echo "make run              启动应用（local 配置）"
	@echo "make test             运行单元测试"
	@echo "make build            构建可执行 JAR"
	@echo "make clean            清理构建产物"
	@echo "make docker-build     构建 Docker 镜像"
	@echo "make docker-run       运行 Docker 容器"
	@echo "make docker-stop      停止并删除容器"
	@echo "make help             显示此帮助"

# 启动应用（开发模式）
.PHONY: run
run:
	@echo "🚀 启动 $(APP_NAME) [profile=$(SPRING_PROFILES_ACTIVE), port=$(SERVER_PORT)]"
	$(GRADLE) bootRun \
		--args='--spring.profiles.active=$(SPRING_PROFILES_ACTIVE) --server.port=$(SERVER_PORT)'

# 运行测试
.PHONY: test
test:
	@echo "🧪 运行测试..."
	$(GRADLE) test

# 构建可执行 JAR
.PHONY: build
build:
	@echo "📦 构建 JAR..."
	$(GRADLE) clean build -x test

# 清理
.PHONY: clean
clean:
	@echo "🧹 清理构建目录..."
	$(GRADLE) clean

# ==============================
# Docker 相关
# ==============================

# 构建 Docker 镜像
.PHONY: docker-build
docker-build: build
	@echo "🐋 构建 Docker 镜像: $(DOCKER_IMAGE)"
	docker build -t $(DOCKER_IMAGE) .

# 运行 Docker 容器
.PHONY: docker-run
docker-run: docker-build
	@echo "🚀 运行 Docker 容器 [port=$(SERVER_PORT)]"
	docker run -d \
		--name $(APP_NAME) \
		-p $(SERVER_PORT):8080 \
		-e SPRING_PROFILES_ACTIVE=prod \
		$(DOCKER_IMAGE)

# 停止并删除容器
.PHONY: docker-stop
docker-stop:
	@echo "🛑 停止并删除容器 $(APP_NAME)"
	docker stop $(APP_NAME) 2>/dev/null || true
	docker rm $(APP_NAME) 2>/dev/null || true

# 重新构建并运行
.PHONY: docker-restart
docker-restart: docker-stop docker-run

# ==============================
# 便捷命令
# ==============================

# 生成 JAR 并运行（不使用 Docker）
.PHONY: run-jar
run-jar: build
	@echo "🚀 通过 JAR 运行应用"
	java -jar build/libs/$(APP_NAME)-*.jar --spring.profiles.active=$(SPRING_PROFILES_ACTIVE)

# 查看日志（Docker）
.PHONY: logs
logs:
	docker logs -f $(APP_NAME) 2>/dev/null || echo "容器 $(APP_NAME) 未运行"

# ==============================
# 验证（需配置 Gradle 插件）
# ==============================

# 检查依赖漏洞（需 dependency-check-gradle 插件）
.PHONY: verify
verify:
	@echo "🔍 检查依赖漏洞..."
	$(GRADLE) dependencyCheckAnalyze

# 检查代码格式（需 spotless 插件）
.PHONY: format-check
format-check:
	@echo "📋 检查代码格式..."
	$(GRADLE) spotlessCheck