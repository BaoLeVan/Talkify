// ============================================================
//  MongoDB Initialization Script
//  Database: chat_db
//  Chạy tự động khi container khởi động lần đầu.
// ============================================================

db = db.getSiblingDB('chat_db');

db.createUser({
    user: 'chatuser',
    pwd: 'chatpassword_change_in_prod',
    roles: [{ role: 'readWrite', db: 'chat_db' }]
});

// ============================================================
//  COLLECTION: messages
//
//  Lưu nội dung tin nhắn. _id = Snowflake ID (time-sortable).
//
//  Thiết kế quan trọng:
//  - KHÔNG nhúng reactions (dùng message_reactions riêng — tránh document phình)
//  - KHÔNG nhúng deletedFor (dùng message_deleted riêng — dễ query, không làm to document)
//  - isRevoked + xóa content khi thu hồi, giữ document để sequenceNumber liên tục
//  - replyTo nhúng snapshot (tránh N+1 lookup khi render page 50 messages)
// ============================================================
db.createCollection('messages', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['_id', 'conversationId', 'sequenceNumber', 'senderId', 'type', 'createdAt'],
            properties: {
                _id:            { bsonType: 'long',   description: 'Snowflake ID = messageId' },
                conversationId: { bsonType: 'string', description: 'UUID của conversation' },
                sequenceNumber: { bsonType: 'long',   description: 'Thứ tự trong conversation, tăng dần từ Redis INCR' },
                senderId:       { bsonType: 'long',   description: 'Snowflake ID của user gửi' },
                type: {
                    bsonType: 'string',
                    enum: ['TEXT', 'IMAGE', 'FILE', 'AUDIO', 'VIDEO', 'SYSTEM']
                },
                content: {
                    bsonType: 'object',
                    properties: {
                        text:        { bsonType: ['string', 'null'] },
                        attachments: { bsonType: 'array' },
                        metadata:    { bsonType: 'object' }
                    }
                },
                // replyTo: snapshot của message gốc (tránh N+1 query khi render)
                replyTo: {
                    bsonType: ['object', 'null'],
                    properties: {
                        messageId:      { bsonType: 'long' },
                        senderId:       { bsonType: 'long' },
                        senderName:     { bsonType: 'string' },
                        type:           { bsonType: 'string' },
                        contentPreview: { bsonType: ['string', 'null'] },
                        isRevoked:      { bsonType: 'bool' }
                    }
                },
                isRevoked: { bsonType: 'bool' },
                revokedAt: { bsonType: ['date', 'null'] },
                revokedBy: { bsonType: ['long', 'null'] },   // userId (Snowflake)
                editedAt:  { bsonType: ['date', 'null'] },
                createdAt: { bsonType: 'date' }
            }
        }
    },
    validationAction: 'warn'
});

// Load lịch sử hội thoại — cursor-based pagination (query dùng nhiều nhất)
db.messages.createIndex(
    { conversationId: 1, sequenceNumber: -1 },
    { name: 'idx_conv_seq', background: true }
);

// Load theo time range — sync và tìm kiếm theo ngày
db.messages.createIndex(
    { conversationId: 1, createdAt: -1 },
    { name: 'idx_conv_created', background: true }
);

// Tin nhắn đã gửi của user — profile screen, media gallery
db.messages.createIndex(
    { senderId: 1, createdAt: -1 },
    { name: 'idx_sender_created', background: true }
);

// Cập nhật replyTo.isRevoked khi message gốc bị thu hồi
db.messages.createIndex(
    { 'replyTo.messageId': 1 },
    { name: 'idx_reply_to_msg', background: true, sparse: true }
);

// Full-text search (default_language: 'none' để hỗ trợ tiếng Việt, không stemming sai)
db.messages.createIndex(
    { 'content.text': 'text' },
    { name: 'idx_text_search', background: true, default_language: 'none', sparse: true }
);

print('✓ Collection: messages — 5 indexes');

// ============================================================
//  COLLECTION: message_deleted
//
//  Lưu record khi user xóa tin nhắn "phía mình" (người khác vẫn thấy).
//  Tách collection thay vì nhúng deletedFor[] vào messages vì:
//  - Document messages không bị phình khi nhiều user xóa cùng 1 tin
//  - Dễ cleanup khi conversation bị xóa
//
//  Flow load lịch sử:
//    1. Query message_deleted: lấy set {messageId} của user trong conversation
//    2. Query messages với sequenceNumber cursor + $nin messageIds đã xóa
// ============================================================
db.createCollection('message_deleted');

// Upsert guard: 1 user chỉ xóa 1 lần / message
db.message_deleted.createIndex(
    { messageId: 1, deletedBy: 1 },
    { unique: true, name: 'idx_msg_deleted_by', background: true }
);

// Load toàn bộ messageId đã xóa của user trong 1 conversation
db.message_deleted.createIndex(
    { conversationId: 1, deletedBy: 1 },
    { name: 'idx_conv_deleted_by', background: true }
);

// Cleanup khi conversation bị xóa hoàn toàn
db.message_deleted.createIndex(
    { conversationId: 1 },
    { name: 'idx_conv_cleanup', background: true }
);

print('✓ Collection: message_deleted — 3 indexes');

// ============================================================
//  COLLECTION: message_reactions
//
//  Lưu reaction của user lên tin nhắn. Tách collection vì:
//  - Group lớn (>500 người) reactions array trong message dễ phình document
//  - 1 user tối đa 1 reaction / message (enforced bởi unique index)
//
//  Flow load reactions khi render page:
//    batch: db.message_reactions.find({ messageId: { $in: [id1, id2, ...] } })
//    → Group by messageId ở application layer
// ============================================================
db.createCollection('message_reactions');

// Upsert: pull old + push new cho cùng (messageId, userId)
db.message_reactions.createIndex(
    { messageId: 1, userId: 1 },
    { unique: true, name: 'idx_msg_user_reaction', background: true }
);

// Load tất cả reactions của 1 message (hoặc batch)
db.message_reactions.createIndex(
    { messageId: 1 },
    { name: 'idx_msg_reactions', background: true }
);

// Cleanup khi conversation bị xóa
db.message_reactions.createIndex(
    { conversationId: 1 },
    { name: 'idx_conv_reactions_cleanup', background: true }
);

print('✓ Collection: message_reactions — 3 indexes');

// ============================================================
//  NOTE: delivery_receipts KHÔNG cần thiết
//
//  READ status được derive hoàn toàn từ PostgreSQL:
//    has_read = participant.last_read_sequence >= message.sequenceNumber
//    who_read = SELECT user_id FROM conversation_participants
//               WHERE last_read_sequence >= :seqNum AND left_at IS NULL
//    unread   = conversation.sequence_counter - participant.last_read_sequence
//
//  DELIVERED state được handle ở application layer (WebSocket ACK),
//  không cần persist vào DB.
// ============================================================

// ============================================================
//  COLLECTION: pending_messages
//
//  Pointer đến messages chờ gửi khi user offline (chỉ lưu messageId, không lưu content).
//  TTL: tự xóa sau 30 ngày — user offline >30 ngày thì sync toàn bộ lịch sử.
// ============================================================
db.createCollection('pending_messages');

// Load pending messages của user khi vừa online
db.pending_messages.createIndex(
    { userId: 1, createdAt: 1 },
    { name: 'idx_user_pending', background: true }
);

// TTL: tự động xóa sau 30 ngày (2592000 giây)
db.pending_messages.createIndex(
    { createdAt: 1 },
    { name: 'idx_ttl_pending', expireAfterSeconds: 2592000, background: true }
);

print('✓ Collection: pending_messages — 2 indexes, TTL 30d');

// ============================================================
//  VERIFICATION
// ============================================================
print('');
print('=== MongoDB Initialization Complete ===');
print('Database  : chat_db');
print('User      : chatuser (readWrite)');
print('Collections:');
print('  - messages          (5 indexes)');
print('  - message_deleted   (3 indexes)');
print('  - message_reactions (3 indexes)');
print('  - pending_messages  (2 indexes, TTL 30d)');
print('');
print('READ tracking → PostgreSQL conversation_participants.last_read_sequence');
print('UNREAD count  → sequence_counter - last_read_sequence (O(1), no DB query)');
