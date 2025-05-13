package com.mindmate.mindmate_server.global.util;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.report.domain.Report;
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

    public void sendMagazineCreateAlert(Magazine magazine, User user) {
        String message = String.format(
                ":newspaper: *새 매거진 등록 요청*\n> 제목: %s (ID: %d)\n> 작성자: %s (ID: %d)\n> 카테고리: %s\n> 생성일: %s",
                magazine.getTitle(), magazine.getId(), user.getProfile().getNickname(), user.getId(),
                magazine.getCategory(), magazine.getCreatedAt()
        );
        sendSlackMessage(message);
    }

    public void sendMagazineUpdateAlert(Magazine magazine, User user) {
        String message = String.format(
                ":pencil2: *매거진 수정 요청*\n> 제목: %s (ID: %d)\n> 작성자: %s (ID: %d)\n> 카테고리: %s\n> 수정일: %s",
                magazine.getTitle(), magazine.getId(), user.getProfile().getNickname(), user.getId(),
                magazine.getCategory(), LocalDateTime.now()
        );
        sendSlackMessage(message);
    }

    public void sendEmoticonUploadAlert(Emoticon emoticon, User creator) {
        try {
            String message = String.format(
                    ":art: *새 이모티콘 등록 요청*\n" +
                            "> 이름: %s (ID: %d)\n" +
                            "> 제작자: %s (ID: %d)\n" +
                            "> 가격: %d 포인트\n" +
                            "> 등록일: %s\n" +
                            "> 이미지: %s",
                    emoticon.getName(),
                    emoticon.getId(),
                    creator.getProfile().getNickname(),
                    creator.getId(),
                    emoticon.getPrice(),
                    emoticon.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    emoticon.getImageUrl()
            );

            sendSlackMessage(message);
            log.info("Slack 이모티콘 등록 알림 전송 완료: emoticonId={}, userId={}", emoticon.getId(), creator.getId());
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패: {}", e.getMessage(), e);
        }
    }

    // SlackNotifier 클래스 내부
    public void sendReportAlert(Report report) {
        try {
            String message = String.format(
                    ":rotating_light: *신고 접수 알림*\n" +
                            "> 신고자: %s (ID: %d)\n" +
                            "> 대상자: %s (ID: %d)\n" +
                            "> 신고 유형: %s\n" +
                            "> 신고 사유: %s\n" +
                            "> 추가 설명: %s\n" +
                            "> 신고 일시: %s",
                    report.getReporter().getProfile().getNickname(),
                    report.getReporter().getId(),
                    report.getReportedUser().getProfile().getNickname(),
                    report.getReportedUser().getId(),
                    report.getReportTarget(),
                    report.getReportReason(),
                    report.getAdditionalComment(),
                    report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            sendSlackMessage(message);
            log.info("신고 알림 전송 완료: reportId={}", report.getId());
        } catch (Exception e) {
            log.error("신고 알림 전송 실패: {}", e.getMessage(), e);
        }
    }

    public void sendKafkaDLQAlert(String alertType, String message) {
        try {
            String formattedMessage = String.format(
                    ":warning: *Kafka DLQ Alert - %s*\n" +
                            "> %s\n" +
                            "DLQ 토픽을 확인하고 조치를 취해주세요!",
                    alertType, message
            );

            sendSlackMessage(formattedMessage);
            log.info("Kafka DLQ 알림 전송 완료: alertType={}", alertType);
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

    private void sendSlackMessage(String message) {
        Map<String, Object> payload = Map.of("text", message);
        restTemplate.postForEntity(webhookUrl, payload, String.class);
    }
}
