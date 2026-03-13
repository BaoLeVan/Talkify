package com.talkify.identity.infrastructure.email;

import com.talkify.identity.application.port.EmailPort;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationEmail(String email, String displayName, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Talkify — Mã xác thực của bạn");
            helper.setText(buildHtml(displayName, otpCode), true);

            mailSender.send(message);
            log.info("Verification email sent to {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", email, e.getMessage());
            // Không re-throw: email fail không nên rollback transaction register
        }
    }

    private String buildHtml(String displayName, String otpCode) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
                    <h2 style="color: #4F46E5;">Talkify</h2>
                    <p>Xin chào <b>%s</b>,</p>
                    <p>Mã xác thực của bạn là:</p>
                    <div style="font-size: 36px; font-weight: bold; letter-spacing: 10px;
                                color: #4F46E5; padding: 16px; background: #F3F4F6;
                                text-align: center; border-radius: 8px;">
                        %s
                    </div>
                    <p style="color: #6B7280; font-size: 13px; margin-top: 16px;">
                        Mã có hiệu lực trong <b>5 phút</b>. Không chia sẻ mã này với bất kỳ ai.
                    </p>
                </div>
                """.formatted(displayName, otpCode);
    }
}
