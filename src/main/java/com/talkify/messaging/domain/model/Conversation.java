package com.talkify.messaging.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.talkify.common.domain.AggregateRoot;
import com.talkify.common.domain.UserId;
import com.talkify.common.exception.AppException;
import com.talkify.common.exception.ErrorCode;
import com.talkify.common.id.IdGenerator;
import com.talkify.messaging.domain.event.ConversationChangedTitleEvent;
import com.talkify.messaging.domain.event.ConversationCreatedEvent;
import com.talkify.messaging.domain.event.LastMessageUpdatedEvent;
import com.talkify.messaging.domain.event.ParticipantAddedEvent;
import com.talkify.messaging.domain.event.ParticipantRemovedEvent;

import lombok.Getter;

/**
 * AGGREGATE ROOT — Conversation
 * 
 * Invariants protected by this aggregate:
 * 1. A DIRECT conversation has exactly 2 participants (no self-conversation)
 * 2. A GROUP conversation requires a non-blank title
 * 3. Only ACTIVE participants can perform actions
 * 4. Only admins/owners can remove other participants
 * 5. Suspended conversations cannot accept new messages
 * 6. sequenceCounter monotonically increases (reflects latest message sequence)
 * 
 * Design decisions:
 * - @Getter only (NO @Setter) — state mutation ONLY through behavior methods
 * - Factory methods (createDirect, createGroup) enforce creation invariants
 * - restore() is for hydration from persistence — bypasses invariant checks
 * - All state transitions emit domain events for eventual consistency
 */
@Getter
public class Conversation extends AggregateRoot {

    // ─── Identity ─────────────────────────────────────────────────────────
    private ConversationId id;
    private ConversationType type;
    private ConversationStatus status;

    // ─── Group-specific ──────────────────────────────────────────────────
    private String title;
    private String avatarUrl;

    // ─── Participants (entity collection, part of this aggregate) ─────────
    private Set<Participant> participants;

    // ─── Last Message Snapshot (denormalized for list query) ──────────────
    private MessageId lastMessageId;
    private UserId lastMessageSenderId;
    private MessageType lastMessageType;
    private String lastMessagePreview;
    private Instant lastMessageAt;

    // ─── Sequence tracking ───────────────────────────────────────────────
    private long sequenceCounter;

    // ─── Audit ───────────────────────────────────────────────────────────
    private Instant createdAt;
    private UserId createdBy;
    private Instant updatedAt;
    private UserId updatedBy;
    private Instant deletedAt;
    private UserId deletedBy;

    // Private constructor — force usage of factory methods or restore()
    private Conversation() {}

    // ═══════════════════════════════════════════════════════════════════════
    // FACTORY METHODS — enforce creation invariants
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Factory: Create a DIRECT conversation between exactly 2 users.
     * 
     * Invariants enforced:
     * - user1 != user2 (no self-conversation)
     * - Exactly 2 participants created as MEMBER role
     */
    public static Conversation createDirect(IdGenerator idGenerator, UserId user1, UserId user2) {
        if (user1.equals(user2)) {
            throw new AppException(ErrorCode.SELF_CONVERSATION);
        }

        Conversation conv = new Conversation();
        conv.id = new ConversationId(idGenerator.nextId());
        conv.type = ConversationType.DIRECT;
        conv.status = ConversationStatus.ACTIVE;
        conv.sequenceCounter = 0L;
        conv.createdAt = Instant.now();
        conv.createdBy = user1;
        conv.participants = new HashSet<>();
        conv.participants.add(Participant.member(user1));
        conv.participants.add(Participant.member(user2));

        conv.registerEvent(new ConversationCreatedEvent(conv.id, ConversationType.DIRECT));
        return conv;
    }

    /**
     * Factory: Create a GROUP conversation.
     * 
     * Invariants enforced:
     * - Title must be non-blank
     * - Creator is automatically OWNER
     * - Other members are added as MEMBER role
     */
    public static Conversation createGroup(IdGenerator idGenerator, UserId creator,
                                           String title, Set<UserId> memberIds) {
        if (title == null || title.isBlank()) {
            throw new AppException(ErrorCode.GROUP_TITLE_REQUIRED);
        }

        Conversation conv = new Conversation();
        conv.id = new ConversationId(idGenerator.nextId());
        conv.type = ConversationType.GROUP;
        conv.status = ConversationStatus.ACTIVE;
        conv.title = title;
        conv.sequenceCounter = 0L;
        conv.createdAt = Instant.now();
        conv.createdBy = creator;
        conv.participants = new HashSet<>();
        conv.participants.add(Participant.owner(creator));

        memberIds.stream()
                .filter(uid -> !uid.equals(creator))
                .forEach(conv::addParticipant);

        conv.registerEvent(new ConversationCreatedEvent(conv.id, ConversationType.GROUP));
        return conv;
    }

    /**
     * Reconstitution factory — hydrate from persistence layer.
     * NO invariant checks (data already validated at creation time).
     * NO events emitted (this is not a state transition).
     */
    public static Conversation restore(ConversationId id, String title, ConversationType type,
                                       ConversationStatus status, String avatarUrl,
                                       Set<Participant> participants,
                                       MessageId lastMessageId, UserId lastMessageSenderId,
                                       MessageType lastMessageType, String lastMessagePreview,
                                       Instant lastMessageAt, Long sequenceCounter,
                                       Instant createdAt, UserId createdBy,
                                       Instant updatedAt, UserId updatedBy) {
        Conversation conv = new Conversation();
        conv.id = id;
        conv.title = title;
        conv.type = type;
        conv.status = status;
        conv.avatarUrl = avatarUrl;
        conv.participants = participants != null ? new HashSet<>(participants) : new HashSet<>();
        conv.lastMessageId = lastMessageId;
        conv.lastMessageSenderId = lastMessageSenderId;
        conv.lastMessageType = lastMessageType;
        conv.lastMessagePreview = lastMessagePreview;
        conv.lastMessageAt = lastMessageAt;
        conv.sequenceCounter = sequenceCounter != null ? sequenceCounter : 0L;
        conv.createdAt = createdAt;
        conv.createdBy = createdBy;
        conv.updatedAt = updatedAt;
        conv.updatedBy = updatedBy;
        return conv;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BEHAVIOR METHODS — state transitions with invariant protection
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Add a new participant to this conversation.
     * Emits ParticipantAddedEvent for downstream processing.
     */
    public void addParticipant(UserId userId) {
        participants.add(Participant.member(userId));
        registerEvent(new ParticipantAddedEvent(id, userId, Instant.now()));
    }

    /**
     * Remove a participant from this conversation.
     * 
     * Invariants:
     * - Actor must be a member
     * - Actor must have admin/owner privileges
     * - Target must be an active member
     */
    public void removeParticipant(UserId actor, UserId target) {
        assertMember(actor);

        participants.stream()
                .filter(p -> p.getUserId().equals(actor) && p.isAdmin())
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_PERMISSION));

        Participant targetParticipant = participants.stream()
                .filter(p -> p.getUserId().equals(target) && p.isActive())
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.NOT_CONVERSATION_MEMBER));

        targetParticipant.leave();
        registerEvent(new ParticipantRemovedEvent(id, target, actor, Instant.now()));
    }

    /**
     * Update the last-message snapshot (denormalized for conversation list query).
     * 
     * Design: Accepts primitives instead of Message aggregate to avoid
     * cross-aggregate direct dependency (Aggregate Boundary Rule).
     * The application layer extracts these values from the Message aggregate.
     */
    public void updateLastMessage(MessageId messageId, UserId senderId,
                                  MessageType messageType, String preview,
                                  Instant sentAt, long sequence) {
        this.lastMessageId = messageId;
        this.lastMessageSenderId = senderId;
        this.lastMessageType = messageType;
        this.lastMessagePreview = preview;
        this.lastMessageAt = sentAt;
        this.sequenceCounter = sequence;
        this.updatedAt = Instant.now();

        registerEvent(new LastMessageUpdatedEvent(this.id, messageId, preview, sentAt));
    }

    /**
     * Change conversation title (GROUP only).
     * 
     * Invariant: Only GROUP conversations can have titles changed.
     */
    public void changeTitle(String newTitle) {
        if (type != ConversationType.GROUP) {
            throw new AppException(ErrorCode.INVALID_CONVERSATION_TYPE);
        }
        if (newTitle == null || newTitle.isBlank()) {
            throw new AppException(ErrorCode.GROUP_TITLE_REQUIRED);
        }
        this.title = newTitle;
        this.updatedAt = Instant.now();
        registerEvent(new ConversationChangedTitleEvent(id, newTitle));
    }

    /**
     * Suspend this conversation (admin action).
     * Suspended conversations cannot accept new messages.
     */
    public void suspend() {
        this.status = ConversationStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    /**
     * Reactivate a suspended conversation.
     */
    public void activate() {
        this.status = ConversationStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // QUERY METHODS — read-only, no state changes
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Assert that userId is an active member. Throws if not.
     * Use this for authorization checks before commands.
     * 
     * Named "assert" (not "is") to clearly communicate it throws.
     */
    public void assertMember(UserId userId) {
        boolean isMember = participants.stream()
                .anyMatch(p -> p.getUserId().equals(userId) && p.isActive());
        if (!isMember) {
            throw new AppException(ErrorCode.NOT_CONVERSATION_MEMBER);
        }
    }

    /**
     * Assert this conversation is not suspended.
     * Call before accepting a new message.
     */
    public void assertNotSuspended() {
        if (status == ConversationStatus.SUSPENDED) {
            throw new AppException(ErrorCode.CONVERSATION_SUSPENDED);
        }
    }

    /**
     * Check if a user is an active member (boolean query, does NOT throw).
     */
    public boolean isMember(UserId userId) {
        return participants.stream()
                .anyMatch(p -> p.getUserId().equals(userId) && p.isActive());
    }

    /**
     * Compute unread message count for a specific participant.
     * Formula: sequenceCounter - participant.lastReadSequence
     */
    public long computeUnreadCount(UserId userId) {
        return participants.stream()
                .filter(p -> p.getUserId().equals(userId) && p.isActive())
                .findFirst()
                .map(p -> Math.max(0, sequenceCounter - p.getLastReadSequence()))
                .orElse(0L);
    }

    /**
     * Find the peer participant in a DIRECT conversation.
     * Returns empty if not DIRECT or peer not found.
     */
    public Optional<Participant> findPeer(UserId currentUserId) {
        if (type != ConversationType.DIRECT) {
            return Optional.empty();
        }
        return participants.stream()
                .filter(p -> p.isActive() && !p.getUserId().equals(currentUserId))
                .findFirst();
    }

    /**
     * Unmodifiable view of participants (protect aggregate boundary).
     */
    public Set<Participant> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }
}
