package com.talkify.common.exception;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    // ── Generic ───────────────────────────────────────────────────────
    INTERNAL_SERVER_ERROR   (HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",  "An unexpected error occurred"),
    INVALID_REQUEST         (HttpStatus.BAD_REQUEST,           "INVALID_REQUEST",         "Invalid request"),
    VALIDATION_FAILED       (HttpStatus.UNPROCESSABLE_CONTENT,  "VALIDATION_FAILED",       "Validation failed"),
    RESOURCE_NOT_FOUND      (HttpStatus.NOT_FOUND,             "RESOURCE_NOT_FOUND",      "Resource not found"),
    DUPLICATE_RESOURCE      (HttpStatus.CONFLICT,              "DUPLICATE_RESOURCE",      "Resource already exists"),
    ACCESS_DENIED           (HttpStatus.FORBIDDEN,             "ACCESS_DENIED",           "Access denied"),
    UNAUTHENTICATED         (HttpStatus.UNAUTHORIZED,          "UNAUTHENTICATED",         "Authentication required"),
    RATE_LIMIT_EXCEEDED     (HttpStatus.TOO_MANY_REQUESTS,     "RATE_LIMIT_EXCEEDED",     "Too many requests"),

    // ── Identity ──────────────────────────────────────────────────────
    USER_MUST_NOT_BE_NULL   (HttpStatus.BAD_REQUEST,           "USER_MUST_NOT_BE_NULL",     "User cannot be null"),
    USER_NOT_FOUND          (HttpStatus.NOT_FOUND,             "USER_NOT_FOUND",          "User not found"),
    USER_ALREADY_EXISTS     (HttpStatus.CONFLICT,              "USER_ALREADY_EXISTS",     "Email or phone number already registered"),
    USER_BANNED             (HttpStatus.FORBIDDEN,             "USER_BANNED",             "Your account has been banned"),
    USER_NOT_VERIFIED       (HttpStatus.FORBIDDEN,             "USER_NOT_VERIFIED",       "Please verify your email first"),
    USER_NAME_ALREADY_EXISTS (HttpStatus.CONFLICT,              "USER_NAME_ALREADY_EXISTS", "Username is already taken"),
    INVALID_CREDENTIALS     (HttpStatus.UNAUTHORIZED,          "INVALID_CREDENTIALS",     "Invalid email or password"),
    INVALID_TOKEN           (HttpStatus.UNAUTHORIZED,          "INVALID_TOKEN",           "Token is invalid or expired"),
    TOKEN_EXPIRED           (HttpStatus.UNAUTHORIZED,          "TOKEN_EXPIRED",           "Token has expired"),
    REFRESH_TOKEN_NOT_FOUND (HttpStatus.UNAUTHORIZED,          "REFRESH_TOKEN_NOT_FOUND", "Refresh token not found"),
    EMAIL_ALREADY_EXISTS    (HttpStatus.CONFLICT,              "EMAIL_ALREADY_EXISTS",     "Email is already in use"),
    OTP_EXPIRED             (HttpStatus.BAD_REQUEST,           "OTP_EXPIRED",             "OTP has expired, please request a new one"),
    OTP_INVALID             (HttpStatus.BAD_REQUEST,           "OTP_INVALID",             "Invalid OTP code"),
    OTP_TOO_MANY_ATTEMPTS   (HttpStatus.TOO_MANY_REQUESTS,     "OTP_TOO_MANY_ATTEMPTS",   "Too many OTP attempts, please request a new one"),
    OTP_MUST_NOT_BE_NULL       (HttpStatus.BAD_REQUEST,           "OTP_MUST_NOT_BE_NULL",     "OTP code cannot be null"),
    OTP_ALREADY_USED           (HttpStatus.BAD_REQUEST,           "OTP_ALREADY_USED",           "OTP code has already been used"),    
    PASSWORD_TOO_WEAK           (HttpStatus.BAD_REQUEST,           "PASSWORD_TOO_WEAK",         "Password must be at least 8 characters long and contain both letters and numbers"),
    
    // ── Messaging ─────────────────────────────────────────────────────
    MESSAGE_NOT_FOUND       (HttpStatus.NOT_FOUND,             "MESSAGE_NOT_FOUND",       "Message not found"),
    MESSAGE_ALREADY_REVOKED (HttpStatus.CONFLICT,              "MESSAGE_ALREADY_REVOKED", "Message has already been revoked"),
    CANNOT_REVOKE_MESSAGE   (HttpStatus.FORBIDDEN,             "CANNOT_REVOKE_MESSAGE",   "You can only revoke your own messages"),
    REVOKE_WINDOW_EXPIRED   (HttpStatus.UNPROCESSABLE_CONTENT,  "REVOKE_WINDOW_EXPIRED",   "Revoke window has expired (15 minutes)"),

    // ── Conversation ──────────────────────────────────────────────────
    CONVERSATION_NOT_FOUND  (HttpStatus.NOT_FOUND,             "CONVERSATION_NOT_FOUND",  "Conversation not found"),
    NOT_CONVERSATION_MEMBER (HttpStatus.FORBIDDEN,             "NOT_CONVERSATION_MEMBER", "You are not a member of this conversation"),

    // ── Group ─────────────────────────────────────────────────────────
    GROUP_NOT_FOUND         (HttpStatus.NOT_FOUND,             "GROUP_NOT_FOUND",         "Group not found"),
    GROUP_FULL              (HttpStatus.UNPROCESSABLE_CONTENT,  "GROUP_FULL",              "Group has reached maximum capacity"),
    INSUFFICIENT_PERMISSION (HttpStatus.FORBIDDEN,             "INSUFFICIENT_PERMISSION", "Insufficient group permissions"),
    ALREADY_GROUP_MEMBER    (HttpStatus.CONFLICT,              "ALREADY_GROUP_MEMBER",    "User is already a member of this group"),
    INVITATION_NOT_FOUND    (HttpStatus.NOT_FOUND,             "INVITATION_NOT_FOUND",    "Invitation not found"),
    INVITATION_ALREADY_USED (HttpStatus.CONFLICT,              "INVITATION_ALREADY_USED", "Invitation has already been responded to"),

    // ── Media ─────────────────────────────────────────────────────────
    FILE_NOT_FOUND          (HttpStatus.NOT_FOUND,             "FILE_NOT_FOUND",          "File not found"),
    FILE_TOO_LARGE          (HttpStatus.CONTENT_TOO_LARGE,     "FILE_TOO_LARGE",          "File size exceeds the allowed limit"),
    UNSUPPORTED_MEDIA_TYPE  (HttpStatus.UNSUPPORTED_MEDIA_TYPE,"UNSUPPORTED_MEDIA_TYPE",  "File type is not supported"),
    UPLOAD_FAILED           (HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_FAILED",           "File upload failed");

    HttpStatus status;
    String code;
    String message;
}