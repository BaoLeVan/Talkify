package com.talkify.messaging.interfaces.rest.response;

import java.time.Instant;

import com.talkify.messaging.domain.model.ConversationType;

/**
 * Response cho mỗi conversation trong danh sách.
 *
 * <ul>
 *   <li>name     — DIRECT: peer displayName hoặc nickname; GROUP: title</li>
 *   <li>avatar   — DIRECT: peer avatarUrl; GROUP: conversation avatarUrl</li>
 *   <li>peer     — chỉ có ở DIRECT; null ở GROUP</li>
 *   <li>preview  — structured, FE tự format thành chuỗi hiển thị theo ngôn ngữ</li>
 *   <li>unreadCount — sequenceCounter - lastReadSequence</li>
 * </ul>
 */
public record ConversationSummaryResponse(
    long id,
    ConversationType type,
    String name,
    String avatar,
    PeerResponse peer,
    MessagePreviewResponse preview,
    Instant lastMessageAt,
    long unreadCount
) {}
