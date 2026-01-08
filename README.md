# 🌱 项目名称

> 一句话描述项目的核心功能（如：一个基于 Spring Boot 的用户管理系统）

---

## 📌 目录

- [特性](#-特性)
- [技术栈](#-技术栈)
- [快速开始](#-快速开始)
- [配置说明](#-配置说明)
- [API 文档](#-api-文档)
- [数据库设计](#-数据库设计)
- [部署指南](#-部署指南)
- [贡献指南](#-贡献指南)
- [许可证](#-许可证)

---

## ✨ 特性

- ✅ **用户认证**：JWT + Spring Security
- ✅ **RESTful API**：符合 OpenAPI 3.0 规范
- ✅ **数据库**：MySQL + JPA + Flyway（自动迁移）
- ✅ **日志**：Logback + ELK 兼容
- ✅ **监控**：Spring Boot Actuator + Prometheus
- ✅ **测试**：JUnit 5 + Mockito + Testcontainers

---

## 🛠 技术栈

| 类别       | 技术                                           |
|----------|----------------------------------------------|
| **核心框架** | Spring Boot 3.4, Spring Web, Spring Data JPA |
| **数据库**  | MySQL 8.0, HikariCP                          |
| **安全**   | Spring Security, JWT                         |
| **构建工具** | Gradle 8.10+                                 |
| **测试**   | JUnit 5, Testcontainers, Mockito             |
| **部署**   | Docker, Docker Compose                       |
| **文档**   | Swagger UI, OpenAPI 3                        |

---

## 🚀 快速开始

### 前置条件
- Java 21+
- Gradle 8.10+
- MySQL 8.0（或使用 Docker）

### 1. 克隆项目
```shell
git clone https://github.com/yourname/your-project.git
cd your-project
```

### 2. 配置数据库

#### 创建数据库:
```sql
CREATE DATABASE user_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 复制本地配置模板：
```shell
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

#### 编辑application-local.yml:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/user_auth
    username: root
    password: your_mysql_password

jwt:
  secret: your-256-bit-super-secret-key-change-in-prod  # 必须 ≥ 32 字符
```

### 3. 启动项目
```shell
# 编译项目
./gradlew clean compileJava

# 启动（启用 local 配置）
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. 验证运行
- #### 健康检查: http://localhost:8080/actuator/health → {"status":"UP"}
- #### Swagger UI: http://localhost:8080/swagger-ui.html
- #### 注册用户:
```shell
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"123456"}'
```

---

## ⚙️ 配置说明

### 配置文件结构

| 文件                        | 说明                     |
|---------------------------|------------------------|
| **application.yml**       | 通用配置（所有环境共享）           |
| **application-local.yml** | 本地开发配置（已加入 .gitignore） |
| **application-prod.yml**  | 生产配置（通过环境变量覆盖）         |

### 关键配置项
```yaml
# JWT 设置
jwt:
  secret: ${JWT_SECRET:default-secret-key}  # 优先从环境变量读取
  expiration: 86400                        # Token 有效期（秒）

# 数据库
spring:
  jpa:
    show-sql: false                        # 生产环境必须关闭
    hibernate:
      ddl-auto: validate                   # 禁止自动建表
  datasource:
    hikari:
      maximum-pool-size: 20

# Actuator 监控
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

## 📚 API 文档

- #### 交互式文档: http://localhost:8080/swagger-ui.html
- #### OpenAPI JSON: http://localhost:8080/v3/api-docs

### 认证方式
所有受保护 API 需在 Header 中携带：
```http request
Authorization: Bearer <your-jwt-token>
```

### 核心 API
| 路径                        | 方法   | 说明          |
|---------------------------|------|-------------|
| **/api/v1/auth/register** | POST | 用户注册        |
| **/api/v1/auth/login**    | POST | 用户登录        |
| **/api/v1/users/me**      | GET  | 获取当前用户信息    |
| **/api/v1/users/{id}**    | PUT  | 更新用户（ADMIN） |

---

## 🗃 数据库设计

### 主要表结构
| 表名             | 说明                 |
|----------------|--------------------|
| **users**      | 用户主表（含用户名、邮箱、密码哈希） |
| **roles**      | 角色表（USER, ADMIN）   |
| **user_roles** | 用户-角色多对多关联表        |

### Flyway 迁移
- #### 脚本位置: src/main/resources/db/migration
- #### 命名规范: V1__init.sql, V2__add_user_email_index.sql

---

## 🚢 部署指南

### Docker 部署
```shell
# 构建镜像
docker build -t user-auth-service .

# 运行容器
docker run -d \
  --name user-auth \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=your-prod-secret-key \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/user_auth \
  user-auth-service
```

### Docker Compose（全栈）
```shell
# 启动 MySQL + 应用
docker-compose up -d
```

#### 📁 docker-compose.yml 包含：
- MySQL 8.0 服务
- 应用服务（自动链接数据库）

---

## 🤝 贡献指南

### 欢迎贡献！请遵循以下流程：
1. Fork 本仓库
2. 创建新分支: git checkout -b feat/your-feature
3. 提交代码: git commit -am 'Add some feature'
4. 推送分支: git push origin feat/your-feature
5. 提交 Pull Request

### 代码规范
- 遵循 Google Java Style Guide
- 单元测试覆盖率 ≥ 80%（gradle test 验证）
- 提交信息格式：
  - feat: add user registration API
  - fix: resolve null pointer in login
  - docs: update README

---

## 📄 许可证
```text
MIT License

Copyright (c) 2025 Your Name

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🙏 致谢
- Spring Boot 官方文档
- JWT.io
- Springdoc OpenAPI
- FlywayDB

