package com.mindmate.mindmate_server.auth.service;

import com.mindmate.mindmate_server.global.exception.AuthErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    /**
     * 인증 이메일 보내기
     * 비동기로 처리하여 실제로 바로 메일을 확인 못하더라도 사용자 경험 향상
     */

    @Async
    public void sendVerificationEmail(User user, String token) {
        String subject = "이메일 인증을 완료해주세요";
        String content = getString(token);
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new CustomException(AuthErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private static String getString(String token) {
        String verificationLink = "http://localhost:8080/api/auth/email/verify?token=" + token;

        return String.format(
                    "아래 링크를 클릭하여 이메일 인증을 완료해주세요:<br>"
                    + "<a href='%s'>이메일 인증하기</a><br>"
                    + "이 링크는 24시간 동안 유효합니다.",
                verificationLink
        );
    }

    /**
     * 아이디 찾기, 비밀번호 찾기
     */
}
