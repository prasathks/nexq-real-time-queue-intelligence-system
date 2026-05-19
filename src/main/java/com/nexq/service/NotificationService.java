package com.nexq.service;

import com.nexq.model.*;
import com.nexq.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Async
    public void sendTokenGeneratedNotification(Token token) {
        String subject = "✅ NexQ Token Confirmed - #" + token.getTokenNumber();
        String body = buildTokenGeneratedEmail(token);
        sendEmail(token.getUser(), subject, body, NotificationType.TOKEN_GENERATED, "Token generated");
    }

    @Async
    public void sendTurnApproachingNotification(Token token) {
        String subject = "⏰ NexQ - Your Turn is Approaching! Token #" + token.getTokenNumber();
        String body = buildTurnApproachingEmail(token);
        sendEmail(token.getUser(), subject, body, NotificationType.TURN_APPROACHING, "Turn approaching");
    }

    @Async
    public void sendTokenExpiredNotification(Token token) {
        String subject = "❌ NexQ Token Expired - #" + token.getTokenNumber();
        String body = buildTokenExpiredEmail(token);
        sendEmail(token.getUser(), subject, body, NotificationType.TOKEN_EXPIRED, "Token expired");
    }

    private void sendEmail(User user, String subject, String htmlBody,
                           NotificationType type, String logMsg) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom("noreply@nexq.com");
            mailSender.send(message);

            saveNotification(user, logMsg + " for token", type);
            log.info("Email sent to {} - {}", user.getEmail(), subject);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void saveNotification(User user, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    private String buildTokenGeneratedEmail(Token token) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:30px;
                            background:#f8f9fa;border-radius:12px;border:1px solid #e0e0e0">
                  <div style="text-align:center;margin-bottom:20px">
                    <h1 style="color:#6c5ce7;font-size:28px;margin:0">NexQ</h1>
                    <p style="color:#636e72;margin:5px 0">Queue Management System</p>
                  </div>
                  <div style="background:white;padding:24px;border-radius:8px;text-align:center">
                    <h2 style="color:#2d3436">Your Token is Confirmed!</h2>
                    <div style="background:#6c5ce7;color:white;font-size:48px;font-weight:bold;
                                padding:20px;border-radius:8px;margin:20px 0">#%d</div>
                    <p style="color:#636e72">Queue: <strong>%s</strong></p>
                    <p style="color:#636e72">Expires: <strong>%s</strong></p>
                    <p style="color:#b2bec3;font-size:12px">Please arrive before your token expires</p>
                  </div>
                </div>
                """.formatted(token.getTokenNumber(), token.getQueue().getName(), token.getExpiresAt());
    }

    private String buildTurnApproachingEmail(Token token) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:30px;
                            background:#fff3cd;border-radius:12px;border:1px solid #ffc107">
                  <div style="text-align:center;margin-bottom:20px">
                    <h1 style="color:#6c5ce7">NexQ</h1>
                  </div>
                  <div style="background:white;padding:24px;border-radius:8px;text-align:center">
                    <h2 style="color:#856404">⏰ Your Turn is Coming Up!</h2>
                    <p>Token <strong>#%d</strong> in <strong>%s</strong></p>
                    <p style="color:#856404">Please proceed to the counter within the next few minutes.</p>
                  </div>
                </div>
                """.formatted(token.getTokenNumber(), token.getQueue().getName());
    }

    private String buildTokenExpiredEmail(Token token) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:30px;
                            background:#f8d7da;border-radius:12px;border:1px solid #f5c6cb">
                  <div style="text-align:center;margin-bottom:20px">
                    <h1 style="color:#6c5ce7">NexQ</h1>
                  </div>
                  <div style="background:white;padding:24px;border-radius:8px;text-align:center">
                    <h2 style="color:#721c24">❌ Token Expired</h2>
                    <p>Token <strong>#%d</strong> in <strong>%s</strong> has expired.</p>
                    <p style="color:#721c24">Please rejoin the queue if you still need service.</p>
                  </div>
                </div>
                """.formatted(token.getTokenNumber(), token.getQueue().getName());
    }
}
