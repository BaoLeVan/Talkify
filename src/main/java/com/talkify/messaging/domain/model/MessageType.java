package com.talkify.messaging.domain.model;

/**
 * Enumeration of supported message types.
 * 
 * Strategy pattern ready: each type can have different validation rules,
 * preview generation, and storage strategies. When adding a new type,
 * extend MessageContentValidator (future) and preview logic.
 */
public enum MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    /** System-generated messages (user joined, title changed, etc.) */
    SYSTEM
}
