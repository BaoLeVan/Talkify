package com.talkify.identity.application.handler;

import com.talkify.identity.application.command.SendOtpCommand;
import com.talkify.identity.domain.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtpEventListener {

    private final OtpHandler otpHandler;

    /**
     * Lắng nghe UserRegisteredEvent và gửi OTP SAU KHI DB commit thành công.
     *
     * - AFTER_COMMIT: đảm bảo user đã tồn tại trong DB trước khi gửi OTP.
     * - @Async: không block transaction thread.
     *
     * Nếu gửi OTP thất bại sau commit, user đã được lưu nhưng chưa nhận OTP.
     * → User có thể dùng tính năng "Gửi lại OTP" để nhận lại.
     */
    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Handling UserRegisteredEvent AFTER_COMMIT | email={}", event.email());
        try {
            otpHandler.handle(new SendOtpCommand(event.email(), event.otpPurpose()));
        } catch (Exception e) {
            // Log nhưng không re-throw: user vẫn đã được tạo, có thể dùng resend OTP
            log.error("Failed to send OTP after registration | email={} error={}",
                    event.email(), e.getMessage());
        }
    }
}
