package com.talkify.messaging.interfaces.rest.response;

import com.talkify.messaging.domain.model.MessageType;

/**
 * Structured last-message preview — intentionally NOT formatted as a display string.
 *
 * <p>The FE is responsible for composing the final display text in the user's language:
 * <pre>
 *   sentByMe=true  → "Bạn" / "You" / "Tu" + localized verb
 *   sentByMe=false → senderName (null for DIRECT) + localized verb
 *   type=TEXT      → show {@code text} directly
 *   type=IMAGE     → "[📷] Photo" / "Ảnh" / ...
 *   type=VIDEO     → "[🎥] Video"
 *   type=AUDIO     → "[🎤] Voice message"
 *   type=FILE      → "[📎] File"
 * </pre>
 *
 * @param type       message type — tells FE which icon and localized verb to use
 * @param text       raw text content; non-null only when {@code type == TEXT}
 * @param sentByMe   true if the current authenticated user sent this message
 * @param senderName display name of the sender:
 *                   <ul>
 *                     <li>DIRECT: always {@code null} (only 2 participants, redundant)</li>
 *                     <li>GROUP + sentByMe: {@code null} (FE localizes "Bạn" itself)</li>
 *                     <li>GROUP + !sentByMe: resolved display name of the sender</li>
 *                   </ul>
 */
public record MessagePreviewResponse(
    MessageType type,
    String text,
    boolean sentByMe,
    String senderName
) {

    /** No last message yet. */
    public static MessagePreviewResponse empty() {
        return new MessagePreviewResponse(null, null, false, null);
    }
}
