# Talkify

Ứng dụng chat thời gian thực được xây dựng theo kiến trúc **DDD (Domain-Driven Design)** + **Hexagonal Architecture**, sử dụng Spring Boot 4 với đầy đủ tính năng nhắn tin, quản lý nhóm, xác thực OTP, và hỗ trợ scale WebSocket ngang.

---

## Mục lục

- [Tech Stack](#tech-stack)
- [Kiến trúc](#kiến-trúc)
- [Yêu cầu](#yêu-cầu)
- [Cài đặt & Chạy](#cài-đặt--chạy)
  - [1. Cấu hình biến môi trường](#1-cấu-hình-biến-môi-trường)
  - [2. Chạy toàn bộ với Docker Compose](#2-chạy-toàn-bộ-với-docker-compose)
  - [3. Chạy local (không Docker)](#3-chạy-local-không-docker)
- [Build](#build)
- [Test](#test)
- [API nhanh](#api-nhanh)
- [Cấu trúc dự án](#cấu-trúc-dự-án)

---

## Tech Stack

| Layer | Công nghệ |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Auth | JWT (jjwt 0.12.6) + OTP via Email |
| ORM | Spring Data JPA (Hibernate 7) |
| Message Store | MongoDB 7 |
| Cache / Presence | Redis 7 |
| Metadata DB | PostgreSQL 16 |
| File Storage | MinIO (S3-compatible) *(planned)* |
| Messaging | Apache Kafka *(planned)* |
| Container | Docker + Docker Compose |
| Testing | JUnit 5 + Mockito + AssertJ |

---

## Kiến trúc

```
src/main/java/com/talkify/
├── identity/               # Bounded context: Auth, User
│   ├── domain/             # Entities, Value Objects, Repository interfaces
│   ├── application/        # Handler pattern (XHandler.handle(Command))
│   │   ├── handler/        # RegisterUserHandler, LoginHandler, OtpHandler
│   │   ├── command/        # DTOs vào
│   │   └── port/           # JwtPort, CachePort, EmailPort, …
│   ├── infrastructure/     # Adapters (JPA, Redis, SMTP, JWT impl)
│   └── interfaces/rest/    # Controllers
├── messaging/              # Bounded context: Tin nhắn, cuộc trò chuyện
├── group/                  # Bounded context: Nhóm chat
├── notification/           # Gửi thông báo
├── presence/               # Trạng thái online/offline
├── media/                  # Upload file/ảnh
└── common/                 # Shared: exception, id generate, security utils
```

Mỗi bounded context tuân theo **Ports & Adapters**:  
`interfaces → application → domain ← infrastructure`

---

## Yêu cầu

| Công cụ | Version |
|---|---|
| Java JDK | 21+ |
| Maven | 3.9+ (hoặc dùng `./mvnw` wrapper đi kèm) |
| Docker | 24+ |
| Docker Compose | 2.20+ |

---

## Cài đặt & Chạy

### 1. Cấu hình biến môi trường

Tạo file `.env` tại thư mục gốc:

```env
# Gmail App Password (bật 2FA rồi tạo tại https://myaccount.google.com/apppasswords)
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# JWT secret — chuỗi ít nhất 32 ký tự (256-bit)
# Tạo nhanh: openssl rand -base64 32
JWT_SECRET_KEY=your-very-secret-key-at-least-32-chars
```

### 2. Chạy toàn bộ với Docker Compose

```bash
# Khởi động infra + app (build image nếu chưa có)
docker compose up -d

# Chỉ khởi động infra (postgres, mongodb, redis), không build app
docker compose up -d postgres mongodb redis

# Build lại image app sau khi sửa code
docker compose up -d --build app

# Xem log app realtime
docker compose logs -f app

# Dừng tất cả
docker compose down

# Dừng và xóa toàn bộ volumes (reset data)
docker compose down -v
```

Sau khi khởi động, API sẵn sàng tại: `http://localhost:8080`

### 3. Chạy local (không Docker)

Yêu cầu infra (postgres, mongodb, redis) đang chạy. Bạn có thể chỉ khởi động infra:

```bash
docker compose up -d postgres mongodb redis
```

Sau đó chạy Spring Boot:

```bash
# Linux / macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export MAIL_USERNAME=your@gmail.com
export MAIL_PASSWORD=your_app_password
export JWT_SECRET_KEY=your-secret-key

./mvnw spring-boot:run
```

```powershell
# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"
$env:MAIL_USERNAME = "your@gmail.com"
$env:MAIL_PASSWORD = "your_app_password"
$env:JWT_SECRET_KEY = "your-secret-key"

.\mvnw.cmd spring-boot:run
```

---

## Build

```bash
# Chạy build thường (có compile & test)
./mvnw clean package

# Build nhanh, bỏ qua test
./mvnw clean package -DskipTests

# Build thành JAR và chạy trực tiếp
./mvnw clean package -DskipTests
java -jar target/talkify-0.0.1-SNAPSHOT.jar

# Build Docker image thủ công
docker build -t talkify:latest .
```

---

## Test

### Chạy tất cả test

```bash
./mvnw test
```

### Chạy test theo package / pattern

```bash
# Tất cả handler tests (identity module)
./mvnw test -Dtest="com.talkify.identity.application.handler.*Test"

./mvnw test -Dtest="com.talkify.identity.application.handler.*Test" -Dsurefire.useFile=false
```

### Chạy một test class cụ thể

```bash
./mvnw test -Dtest=RegisterUserHandlerTest

./mvnw test -Dtest=LoginHandlerTest

./mvnw test -Dtest=OtpHandlerTest
```

### Chạy một method test cụ thể

```bash
./mvnw test -Dtest=OtpHandlerTest#bruteForce

./mvnw test -Dtest=LoginHandlerTest#inactiveUser
```

### Chạy test với báo cáo coverage (JaCoCo)

```bash
./mvnw test jacoco:report
# Báo cáo HTML: target/site/jacoco/index.html
```

### Lưu ý khi chạy test

Nếu máy có nhiều JDK, cần đảm bảo dùng **Java 21**:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version  # phải ra 21.x
./mvnw test
```

---

## API nhanh

Base URL: `http://localhost:8080/api/v1`

### Auth

| Method | Endpoint | Mô tả | Auth |
|---|---|---|---|
| `POST` | `/auth/register` | Đăng ký tài khoản, trả về JWT (INACTIVE) | ✗ |
| `POST` | `/auth/login` | Đăng nhập bằng email / username / SĐT | ✗ |
| `POST` | `/auth/verify-otp` | Xác thực OTP, kích hoạt tài khoản | JWT |
| `POST` | `/auth/resend-otp` | Gửi lại OTP (cooldown 1 phút) | JWT |

#### Ví dụ: Đăng ký

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "username": "myusername",
    "password": "Abcdef12",
    "confirmPassword": "Abcdef12",
    "displayName": "My Name"
  }'
```

#### Ví dụ: Đăng nhập

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "user@example.com",
    "password": "Abcdef12"
  }'
```

> `identifier` có thể là **email**, **số điện thoại** (`+84xxxxxxxxx`), hoặc **username**.

#### Ví dụ: Verify OTP

```bash
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "otp": "123456",
    "purpose": "REGISTRATION"
  }'
```

### Flow xác thực tài khoản mới

```
POST /register  →  nhận JWT (status=INACTIVE)
                →  email nhận OTP (TTL 5 phút)
POST /verify-otp (JWT)  →  tài khoản ACTIVE
                        →  JWT mới với status=ACTIVE
```

---

## Cấu trúc dự án

```
Talkify/
├── src/
│   ├── main/
│   │   ├── java/com/talkify/
│   │   │   ├── TalkifyApplication.java
│   │   │   ├── identity/          # Auth, User management
│   │   │   ├── messaging/         # Chat messages
│   │   │   ├── group/             # Group chats
│   │   │   ├── notification/      # Push/email notifications
│   │   │   ├── presence/          # Online status
│   │   │   ├── media/             # File uploads
│   │   │   ├── common/            # Shared utilities
│   │   │   ├── config/            # Spring Security, CORS
│   │   │   └── bootstrap/         # App startup config
│   │   └── resources/
│   │       └── application.yaml
│   └── test/
│       └── java/com/talkify/
│           └── identity/application/handler/
│               ├── RegisterUserHandlerTest.java  (9 tests)
│               ├── LoginHandlerTest.java         (12 tests)
│               └── OtpHandlerTest.java           (20 tests)
├── docker/
│   ├── postgres/init.sql          # Schema khởi tạo
│   └── mongodb/init.js            # Index khởi tạo
├── docs/                          # Tài liệu thiết kế
├── dockerfile
├── docker-compose.yml
├── pom.xml
└── .env                           # Biến môi trường (KHÔNG commit)
```

---

## Tài liệu thiết kế

Xem thêm trong thư mục [`docs/`](docs/):

| File | Nội dung |
|---|---|
| [01.overview.md](docs/01.overview.md) | Kiến trúc tổng thể, WebSocket scale |
| [02.domain-ddd.md](docs/02.domain-ddd.md) | Domain model, DDD boundaries |
| [03.tech-stack.md](docs/03.tech-stack.md) | Chi tiết tech stack |
| [04.api-design.md](docs/04.api-design.md) | API design |
| [05.solutions.md](docs/05.solutions.md) | Giải pháp kỹ thuật |
| [06.road-map.md](docs/06.road-map.md) | Roadmap |
| [07.db-design.md](docs/07.db-design.md) | Database schema |
