package com.mindmate.mindmate_server.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class KafkaTopicUtils {
    private static final String DLT_SUFFIX = "-dlt";
    private static final String RETRY_SUFFIX_PATTERN = "-retry-\\d+";

    public String extractOriginalTopic(String dltTopic) {
        if (dltTopic == null || !isDltTopic(dltTopic)) {
            return dltTopic;
        }

        // 패턴: {originalTopic}-{consumerGroupPrefix}-group-dlt
        Pattern pattern = Pattern.compile("(.*?)-(\\w+)-group-dlt$");
        Matcher matcher = pattern.matcher(dltTopic);
        if (matcher.find()) {
            String possibleTopic = matcher.group(1);

            List<String> knownTopics = Arrays.asList(
                    "chat-message-topic",
                    "chat-room-close-topic",
                    "magazine-engagement-topic",
                    "matching-accepted"
            );

            for (String baseTopic : knownTopics) {
                if (possibleTopic.equals(baseTopic)) {
                    return baseTopic;
                }
            }

            return possibleTopic;
        }

        // 일반 DLT 패턴: original-topic-dlt
        if (dltTopic.endsWith(DLT_SUFFIX)) {
            return dltTopic.substring(0, dltTopic.length() - DLT_SUFFIX.length());
        }

        log.warn("알 수 없는 DLT 토픽 패턴: {}", dltTopic);
        return dltTopic;
    }

    /**
     * DLT 토픽 이름에서 컨슈머 그룹 이름 추출
     */
    public String extractConsumerGroup(String dltTopic) {
        if (dltTopic == null || !isDltTopic(dltTopic)) {
            return null;
        }

        // 패턴: {originalTopic}-{consumerGroupPrefix}-group-dlt
        Pattern pattern = Pattern.compile(".*?-(\\w+)-group-dlt$");
        Matcher matcher = pattern.matcher(dltTopic);
        if (matcher.find()) {
            String groupPrefix = matcher.group(1);
            return groupPrefix + "-group";
        }

        log.warn("토픽에서 컨슈머 그룹을 추출할 수 없음: {}", dltTopic);
        return null;
    }

    /**
     * 원본 토픽과 컨슈머 그룹으로 DLT 토픽 이름 생성
     */
    public String buildDltTopicName(String originalTopic, String consumerGroup) {
        // 컨슈머 그룹에서 "-group" 접미사 제거
        String groupPrefix = consumerGroup;
        if (consumerGroup.endsWith("-group")) {
            groupPrefix = consumerGroup.substring(0, consumerGroup.length() - 6);
        }

        return originalTopic + "-" + groupPrefix + "-dlt";
    }

    /**
     * 토픽이 DLT 토픽인지 확인
     */
    public boolean isDltTopic(String topic) {
        return topic != null && topic.endsWith(DLT_SUFFIX);
    }

    /**
     * 토픽이 재시도 토픽인지 확인
     */
    public boolean isRetryTopic(String topic) {
        return topic != null && topic.matches(".*" + RETRY_SUFFIX_PATTERN);
    }

    /**
     * 토픽 카테고리 결정 (Chat, Matching, Magazine 등)
     */
    public String determineTopicCategory(String topic) {
        String originalTopic = extractOriginalTopic(topic);

        if (originalTopic.startsWith("chat-")) {
            return "Chat";
        } else if (originalTopic.startsWith("matching-")) {
            return "Matching";
        } else if (originalTopic.startsWith("magazine-")) {
            return "Magazine";
        }

        return "Other";
    }
}
