---
title: "Talkify — Authentication & Security Review (v2)"
date: 2026-04-02
reviewed-by: Senior Backend Architect & Security Specialist
scope: Identity module — Login / Register / Refresh / Logout / OTP
base-commit: fix/rotation (post CRIT-1 & CRIT-2 fixes applied)
---

# TALKIFY AUTHENTICATION SYSTEM — PRODUCTION SECURITY REVIEW (v2)

> **Context:** Review này được thực hiện trên codebase sau khi 2 bản vá nghiêm trọng
> đã được merge vào branch `fix/rotation`:
> - CRIT-1: Race Condition trong Refresh Token Rotation (lock release trước TX commit)
> - CRIT-2: Session DoS qua Expired Token Logout
>
> Review này đánh giá lại toàn bộ hệ thống từ đầu, bao gồm các vấn đề còn tồn tại,
> phân loại lại mức độ, và đề xuất cải thiện ưu tiên cho môi trường production.

---

## PHẦN 1: TỔNG QUAN KIẾN TRÚC

### 1.1 Điểm Mạnh Đáng Ghi Nhận

| Hạng mục | Đánh giá |
|---|---|
| **Hexagonal Architecture** | Ports & Adapters áp dụng đúng: `JwtPort`, `CachePort`, `SessionCachePort` — domain hoàn toàn độc lập với infrastructure |
| **Session Whitelist + Token Hash** | Chỉ lưu `SHA-256(refreshToken)` vào DB — không bao giờ lưu raw token |
| **AFTER_COMMIT Event cho Cache** | `SessionEventListener` dùng `@TransactionalEventListener(AFTER_COMMIT)` — tránh cache pollution khi DB rollback |
| **Hybrid Refresh Strategy** | Reactive renewal (giữ RT khi còn TTL dài) + Proactive rotation (khi gần hết hạn) — cân bằng tốt giữa UX và bảo mật |
| **Reuse Detection** | Phát hiện token reuse và revoke toàn bộ session của user — đúng chuẩn OAuth2 Token Rotation |
| **HttpOnly Cookie cho Refresh Token** | RT không accessible qua JavaScript — phòng XSS |
| **CRIT-1 FIXED** | Lock release đã chuyển sang `AFTER_COMMIT` event — đóng cửa sổ race condition giữa finally và TX commit |
| **CRIT-2 FIXED** | `/logout` bỏ khỏi `SKIP_FILTER_PATHS` và `PUBLIC_POST` — userId/sessionId lấy từ SecurityContext principal đã xác thực |
| **Domain Model có Behavior** | `User.activate()`, `User.changePassword()`, `UserSession.revoke()` — không phải anemic model |
| **Scope-based Logout** | `LogoutScope` enum (CURRENT/ALL_EXCEPT_CURRENT/ALL) — thiết kế multi-device đúng đắn |

### 1.2 Luồng Authentication End-to-End

```
REGISTER: POST /register → User(INACTIVE) → AccessToken + RT(cookie) → async OTP email
LOGIN:     POST /login    → verify BCrypt → createSession → AT(body) + RT(cookie)
REFRESH:   GET  /refresh-token (RT cookie) → validate JWT → lookup by SHA256(RT) →
           reuse detection → hybrid rotation → new AT + (new RT nếu gần hết hạn)
LOGOUT:    POST /logout (Bearer AT required) → validate AT tại JwtAuthFilter →
           principal từ SecurityContext → scope-based revoke session(s)
OTP:       POST /verify-otp (Bearer AT required) → rate-limited → atomic GETDEL
```

### 1.3 Sơ Đồ Bảo Vệ Tại JwtAuthFilter

```
Request → extractToken()
        → parseAccessToken()         [signature + expiry check]
        → check type = "access"
        → sessionCachePort.isSessionValid()  [Redis whitelist per-request]
        → check status (BANNED / DELETED / INACTIVE)
        → set SecurityContext principal
        → doFilter()
```

Session whitelist check tại mọi request là lớp bảo vệ quan trọng,
giải quyết vấn đề "access token không revokeable" của hệ thống JWT thuần túy.

---

## PHẦN 2: DANH SÁCH LỖ HỔNG & RỦI RO

### 🔴 NGHIÊM TRỌNG (P0 — Chặn Production)

---

#### [CRIT-3] Hardcoded MongoDB Credentials trong `application.yaml`

**File:** `src/main/resources/application.yaml` — dòng 21

```yaml
spring:
  data:
    mongodb:
      uri: mongodb+srv://warcraftkun:CmeYUkN0DJHeVBmH@cluster0-baole.6bydbes.mongodb.net/...
                        # ^^^^^^^^^^^ PASSWORD PLAINTEXT TRONG SOURCE CODE
```????????//////////////////////////////////////////////////////

**Mức độ nghiêm trọng:** Tối đa. Password production nằm trong source code,
bị commit vào git history, lộ cho mọi người có quyền đọc repository.

**Attack vector:** Clone repo → có full access MongoDB Atlas cluster.

**Fix ngay lập tức:**
1. Rotate MongoDB password ngay — credential hiện tại phải coi là đã bị lộ.
2. Dùng environment variable:

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}  # inject từ .env hoặc secret manager — KHÔNG commit
```

3. Thêm `.env` vào `.gitignore`, dùng `.env.example` (không có giá trị thật).
4. Bật git secret scanner (GitGuardian, truffleHog) trong CI để phòng tái diễn.
5. Xóa credentials khỏi git history: `git filter-repo --path src/main/resources/application.yaml --force`

---

### 🟠 CAO (P1 — Phải fix trước khi ra production)

---

#### [HIGH-1] `SnowflakeIdGenerator.nextId()` là `synchronized` — Bottleneck Nghiêm Trọng

**File:** `src/main/java/com/talkify/common/id/SnowflakeIdGenerator.java`

```java
@Override
public synchronized long nextId() { ... }  // toàn bộ JVM block tại đây
```

`synchronized` trên instance method tạo ra một global lock trên JVM.
Mọi thread gọi `nextId()` phải xếp hàng tuần tự.
Tại login/register burst, đây là điểm nghẽn duy nhất có thể làm sập hệ thống.

**Pseudo-code fix — Dùng CAS thay synchronized:**

```java
// Tốt nhất: dùng thư viện production-grade
// com.github.f4b6a3:tsid-creator — time-sorted, lock-free, 256 IDs/ms/node
@Component
public class TsidIdGenerator implements IdGenerator {
    private final TsidFactory factory;

    public TsidIdGenerator(@Value("${tsid.node:0}") int node) {
        this.factory = TsidFactory.newInstance256(node);
    }

    @Override
    public long nextId() {
        return factory.create().toLong(); // lock-free, thread-safe
    }
}
```

---

#### [HIGH-2] Không có Rate Limiting trên `/login` và `/register`

Không có giới hạn số lần login thất bại theo IP hoặc identifier.

**Attack vectors:**
- Credential Stuffing: thử hàng triệu username/password từ leaked DB
- Brute-force: BCrypt cost=10 → ~100ms/attempt → 10 threads → 864K attempts/day
- Account Enumeration qua response time khác nhau

**Pseudo-code fix — Redis counter per IP + per identifier:**

```java
// Trong LoginHandler.handle(), trước khi query DB:
String ipKey = "rl:login:ip:" + deviceInfo.ip();
if (cachePort.increment(ipKey, Duration.ofMinutes(15)) > 10) {
    throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
}

String idKey = "rl:login:idf:" + Sha256Utils.hash(command.identifier());
if (cachePort.increment(idKey, Duration.ofHours(1)) > 20) {
    throw new AppException(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
}
```

---

#### [HIGH-3] `cookie.secure=false` là Default — Nguy cơ Token Hijacking

**File:** `src/main/resources/application.yaml`

```yaml
security:
  cookie:
    secure: ${COOKIE_SECURE:false}  # default false nếu quên set env var
```

Nếu `COOKIE_SECURE` không được set trong production (lỗi deployment config),
refresh token cookie gửi qua HTTP không mã hóa — kẻ tấn công có thể sniff.

**Fix — Secure by Default:**
```yaml
secure: ${COOKIE_SECURE:true}  # false chỉ được override rõ ràng ở local dev
```

---

#### [HIGH-4] `tools.jackson.databind.ObjectMapper` — Import Sai Package

**File:** `src/main/java/com/talkify/config/security/JwtAuthFilter.java`

```java
import tools.jackson.databind.ObjectMapper;  // KHÔNG phải Jackson tiêu chuẩn
```

`tools.jackson` là Jackson Databind 3.x (experimental), không tương thích
với Spring Boot auto-configuration dùng `com.fasterxml.jackson`.
Dẫn đến ClassCastException hoặc serialization errors tại runtime.

**Fix:**
```java
import com.fasterxml.jackson.databind.ObjectMapper;
```

---

### 🟡 TRUNG BÌNH (P2 — Fix trước release candidate)

---

#### [MED-1] TTL Shrink Bug trong `CacheAdapter.hset()` — Sessions Bị Mất Sớm

**File:** `src/main/java/com/talkify/identity/infrastructure/cache/CacheAdapter.java`

```java
public void hset(String hashKey, String field, String value, Duration ttl) {
    redisTemplate.opsForHash().put(hashKey, field, value);
    redisTemplate.expire(hashKey, ttl.toSeconds(), TimeUnit.SECONDS); // ghi đè TTL!
}
```

**Scenario:** Session S1 còn 14 ngày. User login thêm device → S2 TTL 7 ngày.
`hset()` set toàn bộ hash về 7 ngày → **S1 bị mất sau 7 ngày dù vẫn hợp lệ.**

`expireIfGreater()` gọi sau đó nhưng đọc TTL vừa bị override → không có tác dụng.

**Fix — Lua Script atomic (HSET + giữ MAX TTL):**

```lua
redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
local current = redis.call('TTL', KEYS[1])
local newTtl  = tonumber(ARGV[3])
if current == -1 or current < newTtl then
    redis.call('EXPIRE', KEYS[1], newTtl)
end
return 1
```

Cũng giải quyết non-atomic HSET+EXPIRE race condition ngay trong cùng một fix.

---

#### [MED-2] OTP Verification Không Constant-Time — Timing Attack

**File:** `src/main/java/com/talkify/identity/application/handler/OtpHandler.java`

```java
if (!cachedCode.equals(command.otp())) { ... }  // String.equals() không constant-time
```

`String.equals()` dừng sớm khi gặp ký tự không khớp → thời gian thực thi
phụ thuộc số ký tự đúng đầu. OTP 6 chữ số có thể bị tấn công timing:
thay vì brute-force 10^6 = 1M combinations, giảm xuống còn 6 × 10 = 60 attempts.

**Fix — Constant-time comparison:**

```java
boolean valid = MessageDigest.isEqual(
    cachedCode.getBytes(StandardCharsets.UTF_8),
    command.otp().getBytes(StandardCharsets.UTF_8)
);
if (!valid) { throw new AppException(ErrorCode.OTP_INVALID); }
```

---

#### [MED-3] OTP Verify Có Double Read — Không Cần Thiết, Gây Race Condition Nhỏ

**File:** `src/main/java/com/talkify/identity/application/handler/OtpHandler.java`

```java
// Lần đọc 1: kiểm tra OTP
String cachedCode = cachePort.get(otpKey).orElseThrow(...);
if (!cachedCode.equals(command.otp())) { throw ... }

// Lần đọc 2: consume
Optional<String> consumed = cachePort.getAndDelete(otpKey);
if (consumed.isEmpty() || !consumed.get().equals(command.otp())) { throw ... }
```

Giữa lần 1 và lần 2, OTP có thể expire. Lần đọc 1 là thừa.

**Fix — Dùng `getAndDelete()` làm lần đọc duy nhất:**

```java
String consumed = cachePort.getAndDelete(otpKey)
    .orElseThrow(() -> new AppException(ErrorCode.OTP_EXPIRED));

if (!MessageDigest.isEqual(consumed.getBytes(UTF_8), command.otp().getBytes(UTF_8))) {
    throw new AppException(ErrorCode.OTP_INVALID);
}
```

---

#### [MED-4] Refresh Token vẫn là JWT — Information Leakage + Shared Signing Key

**File:** `src/main/java/com/talkify/identity/infrastructure/security/JwtAdapter.java`

Refresh token encode `userId` trong `subject` — base64-decode mà không cần key.
Cả access token và refresh token dùng cùng một signing key — nếu key lộ,
kẻ tấn công forge được cả hai loại.

**Fix — Opaque token (random bytes):**

```java
public String issueRefreshToken(UserId userId) {
    // 32 bytes = 256-bit entropy — userId không nhúng vào token
    // lookup qua DB whitelist bằng SHA256(token)
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
}
```

---

#### [MED-5] `SecurityUtils.requireCurrentUserId()` Ném `IllegalStateException` → HTTP 500

**File:** `src/main/java/com/talkify/common/security/SecurityUtils.java`

```java
.orElseThrow(() -> new IllegalStateException("No authenticated user")); // 500!
```

Nếu SecurityContext trống (bug trong filter), hệ thống trả 500 thay vì 401.
Log bị tràn bởi stack traces không cần thiết.

**Fix:**
```java
.orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED)); // 401
```

---

#### [MED-6] IP Spoofing qua `X-Forwarded-For` Header

**File:** `src/main/java/com/talkify/identity/interfaces/rest/DeviceContextExtractor.java`

```java
String xff = request.getHeader(HEADER_X_FORWARDED_FOR);
if (xff != null && !xff.isBlank()) {
    return xff.split(",")[0].trim(); // Client tự inject, không validate
}
```

Client có thể gửi `X-Forwarded-For: 127.0.0.1` để bypass IP-based rate limiting.

**Fix — Chỉ trust XFF từ trusted proxy:**
```java
// Chỉ đọc XFF nếu request đến từ IP của reverse proxy đã biết
if (TRUSTED_PROXY_CIDRS.contains(request.getRemoteAddr())) {
    // parse XFF
}
```

Hoặc dùng `ForwardedHeaderFilter` + `server.forward-headers-strategy=NATIVE`.

---

#### [MED-7] `UserSession` Dùng `Instant.now()` Thay vì Injected `Clock`

**File:** `src/main/java/com/talkify/identity/domain/model/UserSession.java`

```java
public boolean isExpired() { return isExpired(Instant.now()); }  // hardcoded now
public void revoke()       { revoke(Instant.now()); }
public void markUsed()     { markUsed(Instant.now()); }
```

- Unit test không thể kiểm soát "thời gian hiện tại" → không test được edge cases expiry
- `SessionHandler` đã inject `Clock` nhưng `doHandle()` gọi `session.isExpired()` (no-arg)
  thay vì `session.isExpired(clock.instant())`

**Fix — Xóa no-arg overloads, buộc caller inject Instant:**

```java
// Chỉ giữ lại:
public boolean isExpired(Instant now) { return now.isAfter(expiresAt); }
public void revoke(Instant now)       { if (!isRevoked()) this.revokedAt = now; }
public void markUsed(Instant now)     { this.lastUsedAt = now; }
```

---

### 🟢 THẤP (P3 — Technical debt)

---

#### [LOW-1] `jpa.hibernate.ddl-auto: update` Nguy Hiểm Trên Production

```yaml
jpa:
  hibernate:
    ddl-auto: update  # Hibernate tự sửa DB schema
```

`update` cho phép Hibernate thêm cột nhưng không xóa → schema drift khó debug.

**Fix:** Dùng `validate` ở production + **Flyway** hoặc **Liquibase** cho migration.

---

#### [LOW-2] Missing `iss`/`aud` Claims trong JWT

Nếu hệ thống sau này có nhiều service, token issue cho service A
có thể được dùng cho service B.

```java
Jwts.builder()
    .issuer("talkify-auth")
    .audience().add("talkify-api").and()
    // ...
```

---

#### [LOW-3] `LogoutHandler.ALL_EXCEPT_CURRENT` — 2 DB Round-trips

```java
sessionRepository.revokeAllByUserIdExceptSessionId(userId, command.sessionId());
var currentOpt = sessionRepository.findById(command.sessionId()); // query thứ 2
```

`expiresAt` của session hiện tại có thể truyền xuống từ controller (đã biết từ JWT).

---

#### [LOW-4] Missing Device Binding khi Proactive Rotation

Khi rotation xảy ra, không kiểm tra device của request hiện tại
có khớp với device trong session gốc. Nếu RT bị đánh cắp, session mới
sẽ ghi nhận device của kẻ tấn công mà không có alert.

---

#### [LOW-5] `ALLOW_PATHS_FOR_INACTIVE_USER` Tạo Stream Allocation Mỗi Request

```java
List.of(ALLOW_PATHS_FOR_INACTIVE_USER).stream()  // List.of() mỗi request
    .noneMatch(...)
```

Dùng `Set.of()` làm constant để tránh allocation:
```java
private static final Set<String> INACTIVE_ALLOW = Set.of(
    "/api/v1/auth/send-otp", "/api/v1/auth/verify-otp", "/api/v1/auth/resend-otp"
);
```

---

## PHẦN 3: ĐỀ XUẤT CẢI THIỆN CODE

### 3.1 Fix [CRIT-3] — Environment Variables cho Credentials

```yaml
# application.yaml (commit được)
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/chatdb}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD}             # bắt buộc — không có default nguy hiểm
  data:
    mongodb:
      uri: ${MONGODB_URI}                # bắt buộc
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

# .env.example (commit — không có giá trị thật)
# DB_PASSWORD=change_me
# MONGODB_URI=mongodb+srv://USER:PASS@HOST/DB
# JWT_SECRET_KEY=<openssl rand -hex 32>
# COOKIE_SECURE=true
```

### 3.2 Fix [HIGH-2] — Rate Limiter Service

```java
@Service
@RequiredArgsConstructor
public class RateLimitService {
    private final CachePort cachePort;

    // 10 attempts / 15 min / IP
    public void checkLoginByIp(String ip) {
        long count = cachePort.increment("rl:login:ip:" + ip, Duration.ofMinutes(15));
        if (count > 10) throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
    }

    // 20 attempts / 1 hour / identifier (chống brute-force nhắm tài khoản)
    public void checkLoginByIdentifier(String identifier) {
        String key = "rl:login:idf:" + Sha256Utils.hash(identifier);
        long count = cachePort.increment(key, Duration.ofHours(1));
        if (count > 20) throw new AppException(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
    }
}
```

### 3.3 Fix [MED-1] — Atomic Lua Script cho HSET

```java
// CacheAdapter.java
private static final String HSET_MAX_TTL_SCRIPT = """
    redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
    local current = redis.call('TTL', KEYS[1])
    local target  = tonumber(ARGV[3])
    if current == -1 or current < target then
        redis.call('EXPIRE', KEYS[1], target)
    end
    return 1
    """;

@Override
public void hset(String hashKey, String field, String value, Duration ttl) {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>(HSET_MAX_TTL_SCRIPT, Long.class);
    redisTemplate.execute(script, List.of(hashKey),
        field, value, String.valueOf(ttl.toSeconds()));
}
```

### 3.4 Fix [MED-2 + MED-3] — Atomic + Constant-time OTP Verify

```java
@Transactional
public void handle(VerifyOtpCommand command, UserId userId) {
    User user = userRepository.findById(userId.value())
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    command.purpose().assertValidState(user);

    String otpKey     = OtpCacheKey.of(user.getId(), command.purpose()).value();
    String attemptKey = OtpCacheKey.attemptOf(user.getId(), command.purpose()).value();

    long attempts = cachePort.increment(attemptKey, ATTEMPT_TTL);
    if (attempts > MAX_ATTEMPTS) {
        cachePort.delete(otpKey);
        throw new AppException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
    }

    // Atomic GETDEL — consume trong một operation, tránh race condition
    String consumed = cachePort.getAndDelete(otpKey)
            .orElseThrow(() -> new AppException(ErrorCode.OTP_EXPIRED));

    // Constant-time comparison — chống timing attack
    boolean valid = MessageDigest.isEqual(
            consumed.getBytes(StandardCharsets.UTF_8),
            command.otp().getBytes(StandardCharsets.UTF_8)
    );
    if (!valid) throw new AppException(ErrorCode.OTP_INVALID);

    command.purpose().applyEffect(user);
    userRepository.save(user);
    cachePort.delete(attemptKey);
}
```

---

## PHẦN 4: CONCURRENCY — PHÂN TÍCH GIẢI PHÁP HIỆN TẠI

### 4.1 CRIT-1 Đã Được Fix — Phân Tích Chiều Sâu

Giải pháp trong codebase hiện tại là đúng về mặt kỹ thuật:

```java
// SessionHandler.java
@Transactional
public AuthResponse handle(RefreshTokenCommand command, DeviceInfo deviceInfo) {
    // ...
    if (!cachePort.setIfAbsent(lockKey, "1", ROTATION_LOCK_TTL)) {
        throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
    }

    // publishEvent TRƯỚC doHandle → event registered vào Spring TX synchronization
    // dù doHandle() ném exception hay thành công, listener đều được kích hoạt
    eventPublisher.publishEvent(new SessionRotationCompletedEvent(lockKey));
    return doHandle(tokenHash, rawToken, deviceInfo);
    // @Transactional commit XẢY RA SAU KHI method return
}
```

```java
// SessionRotationEventListener.java
@TransactionalEventListener(phase = AFTER_COMMIT)    // lock release SAU DB commit
public void onRotationCommitted(event) { cachePort.delete(event.lockKey()); }

@TransactionalEventListener(phase = AFTER_ROLLBACK)  // tránh stuck lock khi lỗi
public void onRotationRolledBack(event) { cachePort.delete(event.lockKey()); }
```

**Timeline an toàn:**
```
T=0ms:  TX BEGIN
T=1ms:  SETNX lockKey acquired
T=2ms:  publishEvent(SessionRotationCompletedEvent)  [registered for AFTER_COMMIT]
T=5ms:  session.revokeByTokenHash()  [DB write, uncommitted]
T=6ms:  createSession() [DB write, uncommitted]
T=7ms:  method returns
T=8ms:  TX COMMIT  <- DB changes NOW visible to all threads
T=9ms:  AFTER_COMMIT fires -> cachePort.delete(lockKey)  <- LOCK RELEASED

Request-2 chỉ acquire lock được sau T=9ms:
  -> findByTokenHash() -> session đã REVOKED trong DB
  -> reuse detection kích hoạt -> revokeAllByUserId()
  -> 401 REFRESH_TOKEN_REUSE_DETECTED
```

### 4.2 Lớp Phòng Thủ Thứ 2 — Optimistic Locking (Defense-in-Depth)

Thêm `@Version` trên JPA entity để phòng trường hợp Redis unavailable:

```java
@Entity
@Table(name = "user_sessions")
public class UserSessionEntity {
    // ...
    @Version
    private Long version;  // Hibernate check version mỗi UPDATE
}
```

Nếu 2 transaction đồng thời revoke cùng 1 session (cùng version),
chỉ 1 thành công. Transaction thứ 2 nhận `OptimisticLockException`
→ không bao giờ issue 2 access token từ cùng 1 refresh token.

### 4.3 Giám Sát `ROTATION_LOCK_TTL`

TTL = 5 giây là hợp lý nhưng cần monitoring:

```java
// Nếu doHandle() chạy > 3s, log warning để phát hiện DB chậm
long elapsed = Duration.between(start, Instant.now()).toMillis();
if (elapsed > 3000) {
    log.warn("Rotation slow | elapsed={}ms | lockTtl={}ms", elapsed,
             ROTATION_LOCK_TTL.toMillis());
}
```

Nếu `doHandle()` chạy > 5s (lock TTL), lock tự expire nhưng `AFTER_ROLLBACK`
listener sẽ cố xóa key không còn tồn tại — vô hại nhưng cần biết.

---

## PHẦN 5: VẤN ĐỀ ĐẶC THÙ CHAT APPLICATION

### 5.1 WebSocket Authentication — Critical Gap

Hiện tại chưa có implementation. Đây là blocker cho production chat app.

**Kiến trúc đề xuất:**

```java
@Component
@RequiredArgsConstructor
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtPort jwtPort;
    private final SessionCachePort sessionCachePort;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ...,
                                   Map<String, Object> attributes) {
        // Extract AT từ Authorization header (KHÔNG từ query param — logs exposure)
        String token = extractBearerToken(request);
        if (token == null) { response.setStatusCode(UNAUTHORIZED); return false; }

        TokenParseResult result = jwtPort.parseAccessToken(token);
        if (!(result instanceof TokenParseResult.Valid valid)) {
            response.setStatusCode(UNAUTHORIZED); return false;
        }

        UserId    userId    = UserId.of(Long.parseLong(valid.claims().subject()));
        SessionId sessionId = valid.claims().sessionId();

        if (!sessionCachePort.isSessionValid(sessionId, userId)) {
            response.setStatusCode(UNAUTHORIZED); return false;
        }

        // Store principal vào WebSocket session
        attributes.put("principal", new AuthPrincipal(userId, sessionId));
        return true;
    }
}
```

Force-disconnect khi session bị revoke: Redis Pub/Sub publish event → WebSocket handler
unsubscribes và closes connection ngay lập tức.

### 5.2 Message Author từ Principal, Không Từ Client Payload

```java
// WRONG — IDOR risk:
@MessageMapping("/send")
public void sendMessage(@Payload MessageDto dto) {
    messageService.send(dto.senderId(), dto.content()); // client kiểm soát senderId
}

// CORRECT:
@MessageMapping("/send")
public void sendMessage(@Payload MessageDto dto, Principal principal) {
    AuthPrincipal auth = (AuthPrincipal)
        ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
    messageService.send(auth.userId(), dto.content()); // từ authenticated principal
}
```

---

## PHẦN 6: PRODUCTION READINESS CHECKLIST

### P0 — Chặn Deploy (phải xong trước bất kỳ deployment nào)

- [ ] **[CRIT-3]** Rotate MongoDB credentials + xóa khỏi git history
- [ ] **[HIGH-4]** Fix Jackson import: `tools.jackson` → `com.fasterxml.jackson`
- [ ] **[HIGH-3]** Đổi `cookie.secure` default thành `true`
- [ ] Kiểm tra tất cả env vars có đủ và không có default nguy hiểm

### P1 — Trước Release Candidate

- [ ] **[HIGH-1]** Thay `synchronized SnowflakeIdGenerator` → lock-free (TSID hoặc CAS)
- [ ] **[HIGH-2]** Redis rate limiting cho `/login` và `/register`
- [ ] **[MED-1]** Lua script atomic HSET+MAX_TTL
- [ ] **[MED-2]** `MessageDigest.isEqual()` cho OTP comparison
- [ ] **[MED-3]** Đơn giản hóa OTP verify: chỉ dùng `getAndDelete()`
- [ ] **[MED-5]** `SecurityUtils` ném `AppException` thay `IllegalStateException`
- [ ] **[LOW-1]** `ddl-auto: validate` + tích hợp Flyway
- [ ] WebSocket handshake authentication

### P2 — Sprint Tiếp Theo

- [ ] **[MED-4]** Opaque refresh token thay vì JWT
- [ ] **[MED-6]** Trusted proxy check cho X-Forwarded-For
- [ ] **[MED-7]** Xóa no-arg overloads trong `UserSession`, inject Clock
- [ ] **[5.2]** Message author validation từ principal
- [ ] **[LOW-2]** `iss`/`aud` claims trong JWT
- [ ] Micrometer metrics: `auth.login.success`, `auth.token.reuse_detected`, `auth.login.failure`
- [ ] Alert on reuse detection spikes

### P3 — Release 1.1

- [ ] Optimistic Locking trên `UserSessionEntity`
- [ ] Device binding check khi rotation
- [ ] Account lockout after N failed attempts
- [ ] Security headers (HSTS, X-Frame-Options, CSP)
- [ ] Redis circuit-breaker (fallback to DB-only session validation khi Redis down)
- [ ] Session listing UI cho user (xem và revoke device từ xa)

---

## TÓM TẮT ĐIỀU HÀNH

> Kiến trúc tổng thể **mạnh và đúng hướng**. 2 lỗ hổng nghiêm trọng nhất
> (CRIT-1 race condition, CRIT-2 session DoS) đã được vá đúng kỹ thuật.
> **Chặn duy nhất cho production là [CRIT-3]** — MongoDB credentials hardcoded.
> Sau P0, hệ thống đủ điều kiện deploy có giám sát. P1 cần hoàn thiện trước GA.

| Nhóm | Count | Ghi chú |
|---|---|---|
| ~~CRIT-1 (Race Condition Lock)~~ | ~~1~~ | **FIXED** — AFTER_COMMIT event listener |
| ~~CRIT-2 (Session DoS Logout)~~ | ~~1~~ | **FIXED** — SecurityContext principal |
| CRIT (còn lại) | 1 | MongoDB credentials hardcoded |
| HIGH | 4 | Snowflake lock / Rate limit / Cookie secure / Jackson import |
| MEDIUM | 7 | TTL bug / OTP timing / OTP race / Opaque RT / SecurityUtils / IP spoof / Clock |
| LOW | 5 | ddl-auto / iss-aud / DB roundtrip / Stream alloc / Device binding |
| CHAT | 2 | WebSocket auth / Message IDOR |

---

*Review version 2 — 2026-04-02 | Codebase state: branch fix/rotation (post CRIT fixes)*
