package com.talkify.identity.application.port;

public sealed interface TokenParseResult {

    record Valid(TokenClaims claims) implements TokenParseResult {}

    record Expired() implements TokenParseResult {}

    record Invalid() implements TokenParseResult {}
}
