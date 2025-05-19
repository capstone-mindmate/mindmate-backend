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
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackNotifier {
    @Value("${slack.webhook.url}")
    private String webhookUrl;

    @Value("${application.admin.dashboard.url:http://localhost:8080/admin}")
    private String adminDashboardUrl;


    private final RestTemplate restTemplate;
    private final KafkaTopicUtils topicUtils;

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

    /**
     * DLT 알림 전송
     */
    public void sendKafkaDltAlert(String alertType, String dltTopic, long messageCount, String lastMessageTime, List<String> sampleErrors) {
        try {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(":rotating_light: *Kafka DLT 알림 - ").append(alertType).append("*\n\n");

            messageBuilder.append(":information_source: *기본 정보*\n");
            messageBuilder.append("> • DLT 토픽: `").append(dltTopic).append("`\n");
            messageBuilder.append("> • 메시지 수: ").append(messageCount).append("개\n");
            messageBuilder.append("> • 마지막 메시지: ").append(lastMessageTime).append("\n\n");

            if (sampleErrors != null && !sampleErrors.isEmpty()) {
                messageBuilder.append(":bug: *오류 샘플*\n");
                int count = 0;
                for (String error : sampleErrors) {
                    if (count++ >= 3) break;
                    messageBuilder.append("> • ").append(error).append("\n");
                }
                messageBuilder.append("\n");
            }

            String originalTopic = topicUtils.extractOriginalTopic(dltTopic);
            String consumerGroup = topicUtils.extractConsumerGroup(dltTopic);

            messageBuilder.append(":link: *관리 링크*\n");
            String viewUrl = adminDashboardUrl + "/kafka/dlt/" + dltTopic;
            String reprocessUrl = viewUrl + "/reprocess";

            messageBuilder.append("> • <").append(viewUrl).append("|DLT 메시지 확인>\n");
            messageBuilder.append("> • <").append(reprocessUrl).append("|메시지 재처리>\n\n");

            // 추가 정보
            messageBuilder.append(":memo: *참고 사항*\n");
            messageBuilder.append("> 장애 발생 시간: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            messageBuilder.append("> 원본 토픽: `").append(originalTopic).append("`\n");

            if (consumerGroup != null) {
                messageBuilder.append("> 컨슈머 그룹: `").append(consumerGroup).append("`\n");
            }

            sendSlackMessage(messageBuilder.toString());
            log.info("Kafka DLT 알림 전송 완료: topic={}, count={}", dltTopic, messageCount);
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 카프카 브로커 장애 알림
     */
    public void sendKafkaBrokerAlert(String brokerDetails, String errorMessage) {
        try {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(":fire: *Kafka 브로커 장애 알림*\n\n");
            messageBuilder.append("> 브로커: ").append(brokerDetails).append("\n");
            messageBuilder.append("> 오류: ").append(errorMessage).append("\n");
            messageBuilder.append("> 발생 시간: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

            messageBuilder.append(":link: *관리 링크*\n");
            messageBuilder.append("> • <").append(adminDashboardUrl).append("/kafka/health|브로커 상태 확인>\n");

            sendSlackMessage(messageBuilder.toString());
            log.info("Kafka 브로커 장애 알림 전송 완료");
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 컨슈머 그룹 장애 알림
     */
    public void sendConsumerGroupAlert(String groupId, String topic, String errorDetails) {
        try {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(":warning: *Kafka 컨슈머 그룹 장애 알림*\n\n");
            messageBuilder.append("> 컨슈머 그룹: `").append(groupId).append("`\n");
            messageBuilder.append("> 토픽: `").append(topic).append("`\n");
            messageBuilder.append("> 오류 내용: ").append(errorDetails).append("\n");
            messageBuilder.append("> 발생 시간: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

            messageBuilder.append(":link: *관리 링크*\n");
            messageBuilder.append("> • <").append(adminDashboardUrl).append("/kafka/consumer-groups|컨슈머 그룹 상태 확인>\n");

            sendSlackMessage(messageBuilder.toString());
            log.info("Kafka 컨슈머 그룹 장애 알림 전송 완료: groupId={}", groupId);
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
