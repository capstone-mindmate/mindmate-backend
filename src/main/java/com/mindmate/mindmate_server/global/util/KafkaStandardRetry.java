package com.mindmate.mindmate_server.global.util;

import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000),
        dltTopicSuffix = "-dlt",
        retryTopicSuffix = "-retry",
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
)
public @interface KafkaStandardRetry {
}
