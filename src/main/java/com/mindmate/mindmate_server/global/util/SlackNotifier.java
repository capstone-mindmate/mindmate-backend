package com.mindmate.mindmate_server.global.util;

import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackNotifier {
    @Value("${slack.webhook.url}")
    private String webhookUrl;

    private final RestTemplate restTemplate;

    public void sendSuspensionAlert(User user, String reason, Duration duration) {
        try {
            Map<String, Object> payload = new HashMap<>();

            String message = String.format(
                    ":warning: *사용자 정지 알림*\n" +
                            "> 사용자: %s (ID: %d)\n" +
                            "> 이메일: %s\n" +
                            "> 정지 사유: %s\n" +
                            "> 정지 기간: %s\n" +
                            "> 정지 시작 시간: %s\n" +
                            "> 정지 해제 예정: %s",
                    user.getProfile().getNickname(),
                    user.getId(),
                    user.getEmail(),
                    reason,
                    formatDuration(duration),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    user.getSuspensionEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            sendSlackMessage(message);
            log.info("Slack 정지 알림 전송 완료: userId={}", user.getId());
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패: {}", e.getMessage(), e);
        }
    }

    private String formatDuration(Duration duration) {
        if (duration.toDays() > 0) {
            return duration.toDays() + "일";
        } else {
            return duration.toHours() + "시간";
        }
    }

    public void sendMagazineCreateAlert(Magazine magazine, User user) {
        String message = String.format(
                ":newspaper: *새 매거진 등록 요청*\n> 제목: %s\n> 작성자: %s (ID: %d)\n> 카테고리: %s\n> 생성일: %s",
                magazine.getTitle(), user.getProfile().getNickname(), user.getId(),
                magazine.getCategory(), magazine.getCreatedAt()
        );
        sendSlackMessage(message);
    }

    public void sendMagazineUpdateAlert(Magazine magazine, User user) {
        String message = String.format(
                ":pencil2: *매거진 수정 요청*\n> 제목: %s\n> 작성자: %s (ID: %d)\n> 카테고리: %s\n> 수정일: %s",
                magazine.getTitle(), user.getProfile().getNickname(), user.getId(),
                magazine.getCategory(), LocalDateTime.now()
        );
        sendSlackMessage(message);
    }


    private void sendSlackMessage(String message) {
        Map<String, Object> payload = Map.of("text", message);
        restTemplate.postForEntity(webhookUrl, payload, String.class);
    }
}
