-- ============================================================
--  PostgreSQL Initialization Script
--  Database: chat_db
-- ============================================================

-- ─── Extensions ──────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";          -- For LIKE/ILIKE fast search

-- ============================================================
--  TABLE: users
--  Lưu thông tin tài khoản người dùng.
--  PK: Snowflake ID (BIGINT) — time-sortable, 8 bytes, index B-Tree hiệu quả.
--  Soft-delete bằng status='DELETED' để giữ toàn vẹn FK lịch sử.
--  verify_token KHÔNG lưu ở đây — dùng Redis TTL 24h: verify:{token} = userId
-- ============================================================
DROP TABLE IF EXISTS users CASCADE;
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT       PRIMARY KEY,                    -- Snowflake ID
    username        VARCHAR(50)  UNIQUE NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    avatar_url      VARCHAR(500),
    phone_number    VARCHAR(20)  UNIQUE,
    email           VARCHAR(255) UNIQUE,
    password        VARCHAR(255) NOT NULL,                      -- Bcrypt hash
    system_role     VARCHAR(10)  NOT NULL DEFAULT 'USER'
                    CHECK (system_role IN ('USER', 'ADMIN')),   -- Phân quyền hệ thống
    status          VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED', 'DELETED')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Tìm kiếm user theo username (login)
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username     ON users(username);
-- Tìm kiếm user theo số điện thoại
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone        ON users(phone_number)  WHERE phone_number IS NOT NULL;
-- Tìm kiếm user theo email
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email        ON users(email)         WHERE email IS NOT NULL;
-- Full-text search tên hiển thị (ILIKE)
CREATE INDEX        IF NOT EXISTS idx_users_display_trgm ON users USING gin(display_name gin_trgm_ops);

-- ============================================================
--  TABLE: devices
--  Lưu device token để gửi push notification (FCM / APNs).
--  Không lưu MAC address — nhạy cảm, không cần cho push notification.
--  device_token là UNIQUE toàn cục — nếu app reinstall thì UPSERT.
-- ============================================================
CREATE TABLE IF NOT EXISTS devices (
    id              BIGINT       PRIMARY KEY,                    -- Snowflake ID
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_token    VARCHAR(512) NOT NULL,                      -- FCM / APNs token
    platform        VARCHAR(10)  NOT NULL
                    CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    device_name     VARCHAR(100),
    last_active_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_token UNIQUE (device_token)
);

-- Load tất cả devices của 1 user để push notification
CREATE INDEX IF NOT EXISTS idx_devices_user_id ON devices(user_id);

-- ============================================================
--  TABLE: conversations
--  Metadata của cuộc hội thoại (DIRECT hoặc GROUP).
--  PK: UUID — phân tán đều Kafka partition key, không đoán được ID.
--  Messages lưu trong MongoDB, chỉ giữ preview ở đây để render danh sách.
--  sequence_counter: fallback khi Redis restart + dùng tính unread (lazy).
-- ============================================================
CREATE TABLE IF NOT EXISTS conversations (
    id                   VARCHAR(36)  PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    type                 VARCHAR(10)  NOT NULL
                         CHECK (type IN ('DIRECT', 'GROUP')),
    title                VARCHAR(200),                           -- NULL cho DIRECT
    avatar_url           VARCHAR(500),
    created_by           BIGINT       REFERENCES users(id),
    -- Denormalized — cập nhật mỗi khi có tin nhắn mới
    last_message_id      BIGINT,                                -- Snowflake, không FK (MongoDB)
    last_message_preview VARCHAR(200),                          -- Preview hiển thị danh sách
    last_message_at      TIMESTAMPTZ,                           -- Sort danh sách hội thoại
    -- sequence_counter: nguồn fallback của Redis seq:{convId}
    --   • Mục đích 1: unread = sequence_counter - last_read_sequence (1 write/msg)
    --   • Mục đích 2: khôi phục Redis khi restart, tránh duplicate sequence
    sequence_counter     BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Sort danh sách hội thoại theo tin nhắn mới nhất
CREATE INDEX IF NOT EXISTS idx_conv_last_msg_at  ON conversations(last_message_at DESC NULLS LAST);
-- Lookup conversations do user tạo
CREATE INDEX IF NOT EXISTS idx_conv_created_by   ON conversations(created_by);

-- ============================================================
--  TABLE: conversation_participants
--  Thành viên của cuộc hội thoại.
--  PK composite (conversation_id, user_id) — đảm bảo 1 user không join trùng.
--  left_at NOT NULL = đã rời nhóm (soft, giữ lịch sử audit).
--  deleted_at + deleted_from_seq = xóa hội thoại phía mình (người khác vẫn thấy).
-- ============================================================
CREATE TABLE IF NOT EXISTS conversation_participants (
    conversation_id  VARCHAR(36)  NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id          BIGINT       NOT NULL REFERENCES users(id)          ON DELETE CASCADE,
    role             VARCHAR(10)  NOT NULL DEFAULT 'MEMBER'
                     CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    nickname         VARCHAR(100),                              -- Biệt danh riêng trong nhóm
    muted_until      TIMESTAMPTZ,                              -- NULL = không mute
    -- Dùng để tính unread (lazy): unread = conv.sequence_counter - last_read_sequence
    last_read_sequence     BIGINT     NOT NULL DEFAULT 0,
    -- DELIVERED: thiết bị đã nhận tin (WebSocket ACK hoặc FCM)
    -- has_delivered = last_delivered_sequence >= message.sequenceNumber
    -- Người gửi thấy "đã gửi đến" khi last_delivered_sequence của TT cả recipient được cập nhật
    last_delivered_sequence BIGINT     NOT NULL DEFAULT 0,
    -- Xóa hội thoại phía mình: chỉ hiển thị messages sau deleted_from_seq
    -- Khi có tin nhắn mới > deleted_from_seq → conversation tự hiện lại
    deleted_at       TIMESTAMPTZ,                              -- NULL = chưa xóa
    deleted_from_seq BIGINT       NOT NULL DEFAULT 0,
    joined_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    left_at          TIMESTAMPTZ,                              -- NULL = đang trong nhóm
    PRIMARY KEY (conversation_id, user_id)
);

-- Query chính: danh sách conversations của user (dùng nhiều nhất)
-- Covering index bao gồm các cột thường dùng để tránh heap fetch
CREATE INDEX IF NOT EXISTS idx_cp_user_active ON conversation_participants(user_id)
    INCLUDE (conversation_id, last_read_sequence, role, deleted_at, deleted_from_seq)
    WHERE left_at IS NULL;

-- Kiểm tra quyền admin, đếm thành viên
CREATE INDEX IF NOT EXISTS idx_cp_conv_role ON conversation_participants(conversation_id, role)
    WHERE left_at IS NULL;

-- ============================================================
--  TABLE: group_settings
--  Settings bổ sung chỉ dành cho GROUP conversation (quan hệ 1-1).
--  Tách riêng để tránh NULL columns dư thừa trên DIRECT conversations.
-- ============================================================
CREATE TABLE IF NOT EXISTS group_settings (
    conversation_id      VARCHAR(36)  PRIMARY KEY REFERENCES conversations(id) ON DELETE CASCADE,
    max_members          INT          NOT NULL DEFAULT 1000,
    only_admins_send     BOOLEAN      NOT NULL DEFAULT FALSE,   -- Chỉ admin gửi được
    join_approval        BOOLEAN      NOT NULL DEFAULT FALSE,   -- Phải duyệt khi tham gia
    invite_link          VARCHAR(100) UNIQUE,                   -- Code ngắn
    invite_link_expires  TIMESTAMPTZ,                           -- NULL = không hết hạn
    description          TEXT,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Lookup nhóm qua invite link
CREATE INDEX IF NOT EXISTS idx_group_invite_link ON group_settings(invite_link)
    WHERE invite_link IS NOT NULL;

-- ============================================================
--  TABLE: group_invitations
--  Lời mời tham gia nhóm (explicit invite, khác với invite_link).
-- ============================================================
CREATE TABLE IF NOT EXISTS group_invitations (
    id               BIGINT      PRIMARY KEY,                   -- Snowflake ID
    conversation_id  VARCHAR(36) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    inviter_id       BIGINT      NOT NULL REFERENCES users(id),
    invitee_id       BIGINT      NOT NULL REFERENCES users(id),
    status           VARCHAR(10) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    responded_at     TIMESTAMPTZ,
    -- Không cho phép spam invite: 1 lời mời pending/người/nhóm
    CONSTRAINT uq_invitation_pending UNIQUE (conversation_id, invitee_id, status)
);

-- Danh sách lời mời đang chờ của user
CREATE INDEX IF NOT EXISTS idx_invitations_invitee ON group_invitations(invitee_id, status);
-- Danh sách lời mời của nhóm (admin quản lý)
CREATE INDEX IF NOT EXISTS idx_invitations_conv    ON group_invitations(conversation_id, status);

-- ============================================================
--  TABLE: media_files
--  Metadata của file upload. File thực sự lưu trong MinIO/S3.
--  conversation_id KHÔNG có FK constraint — media tồn tại độc lập
--  (có thể forward sang conversation khác, message xóa media vẫn còn).
-- ============================================================
CREATE TABLE IF NOT EXISTS media_files (
    id               VARCHAR(36)  PRIMARY KEY,                  -- UUID
    uploader_id      BIGINT       REFERENCES users(id),
    conversation_id  VARCHAR(36),                               -- Optional context, không FK
    file_name        VARCHAR(255) NOT NULL,
    mime_type        VARCHAR(100) NOT NULL,                     -- image/jpeg, video/mp4 ...
    file_size        BIGINT       NOT NULL,                     -- Bytes (BIGINT > 2GB support)
    storage_path     VARCHAR(500) NOT NULL,                     -- Path trong MinIO
    cdn_url          VARCHAR(500) NOT NULL,
    thumbnail_url    VARCHAR(500),                              -- Chỉ ảnh/video
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Lịch sử upload của user
CREATE INDEX IF NOT EXISTS idx_media_uploader   ON media_files(uploader_id, created_at DESC);
-- Media trong 1 conversation (hiển thị tab ảnh/video)
CREATE INDEX IF NOT EXISTS idx_media_conv       ON media_files(conversation_id, mime_type)
    WHERE conversation_id IS NOT NULL;

-- ============================================================
--  TABLE: refresh_tokens
--  Lưu refresh token (JWT long-lived) để revoke khi logout.
--  Lưu hash SHA-256 thay vì raw token để giảm impact nếu DB bị leak.
-- ============================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT       PRIMARY KEY,                       -- Snowflake ID
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) UNIQUE NOT NULL,                   -- SHA-256 của actual token
    device_id   BIGINT       REFERENCES devices(id) ON DELETE SET NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Kiểm tra token còn hạn và chưa bị revoke
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id)
    WHERE revoked_at IS NULL;

-- ============================================================
--  TRIGGERS: auto-update updated_at
-- ============================================================
CREATE OR REPLACE FUNCTION fn_update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_conversations_updated_at
    BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_group_settings_updated_at
    BEFORE UPDATE ON group_settings
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

-- ============================================================
--  SAMPLE DATA (development only)
-- ============================================================
INSERT INTO users (id, username, display_name, phone_number, email, password)
SELECT 724288609792458752, 'alice', 'Duy Phước', '+84901000001', 'pndphuoc@gmail.com',
       '$2a$12$placeholder_bcrypt_hash_alice'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 724288609792458752);

INSERT INTO users (id, username, display_name, phone_number, email, password)
SELECT 724288609792458753, 'bob', 'Bob Trần', '+84901000002', 'bob@example.com',
       '$2a$12$placeholder_bcrypt_hash_bob'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 724288609792458753);

SELECT 'PostgreSQL schema initialized successfully' AS status;
