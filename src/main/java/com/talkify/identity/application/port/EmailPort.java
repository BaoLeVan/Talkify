package com.talkify.identity.application.port;

public interface EmailPort {
    void sendVerificationEmail(String email, String displayName, String verificationToken);
}
