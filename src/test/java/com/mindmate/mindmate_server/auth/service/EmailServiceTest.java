package com.mindmate.mindmate_server.auth.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;
    @Mock private MimeMessageHelper mimeMessageHelper;

    @InjectMocks
    private EmailService emailService;

    @Nested
    @DisplayName("이메일 전송")
    class EmailSendTest {
        @Test
        @DisplayName("이메일 전송 성공")
        void sendVerificationEmail_Success() throws MessagingException {
            // given
            User user = User.builder()
                    .email("test@example.com")
                    .build();
            String token = "test-token";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            // when
            emailService.sendVerificationEmail(user, token);

            // then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("이메일 전송 실패")
        void sendVerificationEmail_Failed() throws MessagingException {
            // given
            User user = User.builder()
                    .email("test@example.com")
                    .build();
            String token = "test-token";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doAnswer(invocation -> {
                throw new MessagingException("Failed to send email");
            }).when(mailSender).send(any(MimeMessage.class));

            // when & then
            assertThrows(CustomException.class, () -> emailService.sendVerificationEmail(user, token));
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(any(MimeMessage.class));
        }
    }
}
