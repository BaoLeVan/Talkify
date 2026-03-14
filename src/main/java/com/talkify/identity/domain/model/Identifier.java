package com.talkify.identity.domain.model;

public record Identifier(String value, IdentifierType type) {
    public Identifier {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier must not be null or empty");
        }
    }

    public static Identifier of(String value) {
        String trimmedValue = value.trim();

        if (Email.isValid(trimmedValue)) {
            return new Identifier(trimmedValue, IdentifierType.EMAIL);
        } else if (PhoneNumber.isValid(trimmedValue)) {
            return new Identifier(trimmedValue, IdentifierType.PHONE_NUMBER);
        } else if (Username.isValid(trimmedValue)) {
            return new Identifier(trimmedValue, IdentifierType.USERNAME);
        } else {
            throw new IllegalArgumentException("Invalid identifier format: " + value);
        }
    }
}
