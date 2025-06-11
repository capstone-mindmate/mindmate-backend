package com.mindmate.mindmate_server.global.service;

import com.mindmate.mindmate_server.global.dto.EventWrapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.vavr.CheckedFunction0;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResilientEventPublisherTest {

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private CircuitBreaker kafkaCircuitBreaker;
    @Mock private CompletableFuture<SendResult<String, Object>> sendResultFuture;
    @Mock private SendResult<String, Object> sendResult;

    @InjectMocks
    private ResilientEventPublisher resilientEventPublisher;

    private static final String TOPIC = "test-topic";
    private static final String KEY = "test-key";
    private static final String EVENT = "test-event";

    private BlockingQueue<EventWrapper<?>> localBackupQueue;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException, java.util.concurrent.TimeoutException {
        localBackupQueue = new LinkedBlockingQueue<>(10000);
        ReflectionTestUtils.setField(resilientEventPublisher, "localBackupQueue", localBackupQueue);

        when(sendResultFuture.get(500, TimeUnit.MILLISECONDS)).thenReturn(sendResult);
    }

    @Test
    @DisplayName("이벤트 발행 성공 - 키 포함")
    void publishEvent_ShouldPublishSuccessfully_WithKey() throws Throwable {
        // given
        when(kafkaTemplate.send(TOPIC, KEY, EVENT)).thenReturn(sendResultFuture);
        when(kafkaCircuitBreaker.executeCheckedSupplier(any())).thenAnswer(invocation -> {
            CheckedFunction0<?> supplier = invocation.getArgument(0);
            return supplier.apply();
        });

        // when
        assertDoesNotThrow(() -> resilientEventPublisher.publishEvent(TOPIC, KEY, EVENT));

        // then
        verify(kafkaTemplate).send(TOPIC, KEY, EVENT);
        verify(kafkaCircuitBreaker).executeCheckedSupplier(any());
        assertThat(localBackupQueue).isEmpty();
    }

    @Test
    @DisplayName("이벤트 발행 성공 - 키 없음")
    void publishEvent_ShouldPublishSuccessfully_WithoutKey() throws Throwable {
        // given
        when(kafkaTemplate.send(TOPIC, null, EVENT)).thenReturn(sendResultFuture);
        when(kafkaCircuitBreaker.executeCheckedSupplier(any())).thenAnswer(invocation -> {
            CheckedFunction0<?> supplier = invocation.getArgument(0);
            return supplier.apply();
        });

        // when
        assertDoesNotThrow(() -> resilientEventPublisher.publishEvent(TOPIC, EVENT));

        // then
        verify(kafkaTemplate).send(TOPIC, null, EVENT);
        verify(kafkaCircuitBreaker).executeCheckedSupplier(any());
        assertThat(localBackupQueue).isEmpty();
    }

    @Test
    @DisplayName("서킷 브레이커 실행 실패 시 로컬 큐에 저장")
    void publishEvent_ShouldStoreInLocalQueue_WhenCircuitBreakerFails() throws Throwable {
        // given
        when(kafkaCircuitBreaker.executeCheckedSupplier(any()))
                .thenThrow(new RuntimeException("Circuit breaker failed"));

        // when
        assertDoesNotThrow(() -> resilientEventPublisher.publishEvent(TOPIC, KEY, EVENT));

        // then
        assertThat(localBackupQueue).hasSize(1);
        EventWrapper<?> storedEvent = localBackupQueue.peek();
        assertThat(storedEvent.getTopic()).isEqualTo(TOPIC);
        assertThat(storedEvent.getKey()).isEqualTo(KEY);
        assertThat(storedEvent.getEvent()).isEqualTo(EVENT);

        verify(kafkaCircuitBreaker).executeCheckedSupplier(any());
    }

    @Test
    @DisplayName("카프카 전송 타임아웃 시 로컬 큐에 저장")
    void publishEvent_ShouldStoreInLocalQueue_WhenKafkaTimeout() throws Throwable {
        // given
        when(kafkaTemplate.send(TOPIC, KEY, EVENT)).thenReturn(sendResultFuture);
        when(sendResultFuture.get(500, TimeUnit.MILLISECONDS))
                .thenThrow(new TimeoutException("Kafka timeout"));
        when(kafkaCircuitBreaker.executeCheckedSupplier(any())).thenAnswer(invocation -> {
            CheckedFunction0<?> supplier = invocation.getArgument(0);
            return supplier.apply();
        });

        // when
        assertDoesNotThrow(() -> resilientEventPublisher.publishEvent(TOPIC, KEY, EVENT));

        // then
        assertThat(localBackupQueue).hasSize(1);
        EventWrapper<?> storedEvent = localBackupQueue.peek();
        assertThat(storedEvent.getTopic()).isEqualTo(TOPIC);
        assertThat(storedEvent.getKey()).isEqualTo(KEY);
        assertThat(storedEvent.getEvent()).isEqualTo(EVENT);
    }

    @ParameterizedTest
    @DisplayName("다양한 예외 상황에서 로컬 큐 저장")
    @MethodSource("exceptionScenarios")
    void publishEvent_ShouldHandleExceptions(Exception exception, String description) throws Throwable {
        // given
        when(kafkaTemplate.send(TOPIC, KEY, EVENT)).thenReturn(sendResultFuture);
        when(sendResultFuture.get(500, TimeUnit.MILLISECONDS)).thenThrow(exception);
        when(kafkaCircuitBreaker.executeCheckedSupplier(any())).thenAnswer(invocation -> {
            CheckedFunction0<?> supplier = invocation.getArgument(0);
            return supplier.apply();
        });

        // when
        assertDoesNotThrow(() -> resilientEventPublisher.publishEvent(TOPIC, KEY, EVENT));

        // then
        assertThat(localBackupQueue).hasSize(1);
        EventWrapper<?> storedEvent = localBackupQueue.peek();
        assertThat(storedEvent.getTopic()).isEqualTo(TOPIC);
        assertThat(storedEvent.getKey()).isEqualTo(KEY);
        assertThat(storedEvent.getEvent()).isEqualTo(EVENT);
    }

    static Stream<Arguments> exceptionScenarios() {
        return Stream.of(
                Arguments.of(new TimeoutException("Timeout"), "타임아웃 발생"),
                Arguments.of(new ExecutionException("Execution failed", new RuntimeException()), "실행 실패"),
                Arguments.of(new InterruptedException("Interrupted"), "인터럽트 발생"),
                Arguments.of(new RuntimeException("General error"), "일반 런타임 예외")
        );
    }

    @Test
    @DisplayName("로컬 큐 full 시 이벤트 드롭")
    void publishEvent_ShouldDropEvent_WhenLocalQueueFull() throws Throwable {
        // given
        BlockingQueue<EventWrapper<?>> fullQueue = new LinkedBlockingQueue<>(1);
        fullQueue.offer(new EventWrapper<>("dummy", "dummy", "dummy"));
        ReflectionTestUtils.setField(resilientEventPublisher, "localBackupQueue", fullQueue);

        when(kafkaCircuitBreaker.executeCheckedSupplier(any()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        // when
        assertDoesNotThrow(() -> resilientEventPublisher.publishEvent(TOPIC, KEY, EVENT));

        // then
        assertThat(fullQueue).hasSize(1); // 기존 이벤트만 남아있음
        assertThat(fullQueue.peek().getEvent()).isEqualTo("dummy");
    }

    @Test
    @DisplayName("백업 큐 처리 성공 - 키 포함 이벤트")
    void processBackupQueue_ShouldProcessSuccessfully_WithKey() throws Exception {
        // given
        localBackupQueue.offer(new EventWrapper<>(TOPIC, KEY, EVENT));

        when(kafkaTemplate.send(TOPIC, KEY, EVENT)).thenReturn(sendResultFuture);

        // when
        ReflectionTestUtils.invokeMethod(resilientEventPublisher, "processBackupQueue");

        // then
        assertThat(localBackupQueue).isEmpty();
        verify(kafkaTemplate).send(TOPIC, KEY, EVENT);
        verify(sendResultFuture).get(500, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("백업 큐 처리 성공 - 키 없는 이벤트")
    void processBackupQueue_ShouldProcessSuccessfully_WithoutKey() throws Exception {
        // given
        localBackupQueue.offer(new EventWrapper<>(TOPIC, null, EVENT));

        when(kafkaTemplate.send(TOPIC, EVENT)).thenReturn(sendResultFuture);

        // when
        ReflectionTestUtils.invokeMethod(resilientEventPublisher, "processBackupQueue");

        // then
        assertThat(localBackupQueue).isEmpty();
        verify(kafkaTemplate).send(TOPIC, EVENT);
        verify(sendResultFuture).get(500, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("백업 큐 처리 - 혼합 이벤트 (키 있음/없음)")
    void processBackupQueue_ShouldProcessMixedEvents() throws Exception {
        // given
        localBackupQueue.offer(new EventWrapper<>(TOPIC, KEY, "event1"));
        localBackupQueue.offer(new EventWrapper<>(TOPIC, null, "event2"));

        CompletableFuture<SendResult<String, Object>> future1 = mock(CompletableFuture.class);
        CompletableFuture<SendResult<String, Object>> future2 = mock(CompletableFuture.class);

        when(kafkaTemplate.send(TOPIC, KEY, "event1")).thenReturn(future1);
        when(kafkaTemplate.send(TOPIC, "event2")).thenReturn(future2);
        when(future1.get(500, TimeUnit.MILLISECONDS)).thenReturn(sendResult);
        when(future2.get(500, TimeUnit.MILLISECONDS)).thenReturn(sendResult);

        // when
        ReflectionTestUtils.invokeMethod(resilientEventPublisher, "processBackupQueue");

        // then
        assertThat(localBackupQueue).isEmpty();
        verify(kafkaTemplate).send(TOPIC, KEY, "event1");
        verify(kafkaTemplate).send(TOPIC, "event2");
    }

    @Test
    @DisplayName("백업 큐 처리 중 실패 시 재시도 중단")
    void processBackupQueue_ShouldStopOnFailure() throws Exception {
        // given
        localBackupQueue.offer(new EventWrapper<>(TOPIC, KEY, "event1"));
        localBackupQueue.offer(new EventWrapper<>(TOPIC, KEY, "event2"));

        when(kafkaTemplate.send(TOPIC, KEY, "event1")).thenReturn(sendResultFuture);
        when(sendResultFuture.get(500, TimeUnit.MILLISECONDS))
                .thenThrow(new TimeoutException("Timeout"));

        // when
        ReflectionTestUtils.invokeMethod(resilientEventPublisher, "processBackupQueue");

        // then
        assertThat(localBackupQueue).hasSize(2); // 모든 이벤트가 큐에 남아있음
        verify(kafkaTemplate, times(1)).send(any(), any(), any());
    }

    @Test
    @DisplayName("백업 큐 처리 제한 확인 (최대 100개)")
    void processBackupQueue_ShouldRespectProcessingLimit() throws Exception {
        // given
        for (int i = 0; i < 150; i++) {
            localBackupQueue.offer(new EventWrapper<>(TOPIC, KEY, "event" + i));
        }

        when(kafkaTemplate.send(any(), any(), any())).thenReturn(sendResultFuture);

        // when
        ReflectionTestUtils.invokeMethod(resilientEventPublisher, "processBackupQueue");

        // then
        assertThat(localBackupQueue).hasSize(50); // 100개만 처리되고 50개 남음
        verify(kafkaTemplate, times(100)).send(any(), any(), any());
        verify(sendResultFuture, times(100)).get(500, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("빈 백업 큐 처리")
    void processBackupQueue_ShouldHandleEmptyQueue() {
        // given
        assertThat(localBackupQueue).isEmpty();

        // when & then
        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(resilientEventPublisher, "processBackupQueue")
        );

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("백업 큐 처리 중 일부 성공, 일부 실패")
    void processBackupQueue_ShouldHandlePartialSuccess() throws Exception {
        // given
        localBackupQueue.offer(new EventWrapper<>(TOPIC, KEY, "success1"));
        localBackupQueue.offer(new EventWrapper<>(TOPIC, KEY, "success2"));
        localBackupQueue.offer(new EventWrapper<>(TOPIC, KEY, "fail"));
        localBackupQueue.offer(new EventWrapper<>(TOPIC, KEY, "notProcessed"));

        CompletableFuture<SendResult<String, Object>> successFuture1 = mock(CompletableFuture.class);
        CompletableFuture<SendResult<String, Object>> successFuture2 = mock(CompletableFuture.class);
        CompletableFuture<SendResult<String, Object>> failFuture = mock(CompletableFuture.class);

        when(kafkaTemplate.send(TOPIC, KEY, "success1")).thenReturn(successFuture1);
        when(kafkaTemplate.send(TOPIC, KEY, "success2")).thenReturn(successFuture2);
        when(kafkaTemplate.send(TOPIC, KEY, "fail")).thenReturn(failFuture);

        when(successFuture1.get(500, TimeUnit.MILLISECONDS)).thenReturn(sendResult);
        when(successFuture2.get(500, TimeUnit.MILLISECONDS)).thenReturn(sendResult);
        when(failFuture.get(500, TimeUnit.MILLISECONDS)).thenThrow(new RuntimeException("Send failed"));

        // when
        ReflectionTestUtils.invokeMethod(resilientEventPublisher, "processBackupQueue");

        // then
        assertThat(localBackupQueue).hasSize(2);
        verify(kafkaTemplate, times(3)).send(any(), any(), any());
    }

    @Test
    @DisplayName("Null 이벤트 처리")
    void publishEvent_ShouldHandleNullEvent() throws Throwable {
        // given
        when(kafkaTemplate.send(TOPIC, KEY, null)).thenReturn(sendResultFuture);
        when(kafkaCircuitBreaker.executeCheckedSupplier(any())).thenAnswer(invocation -> {
            CheckedFunction0<?> supplier = invocation.getArgument(0);
            return supplier.apply();
        });

        // when
        assertDoesNotThrow(() -> resilientEventPublisher.publishEvent(TOPIC, KEY, null));

        // then
        verify(kafkaTemplate).send(TOPIC, KEY, null);
        assertThat(localBackupQueue).isEmpty();
    }

    @Test
    @DisplayName("빈 문자열 토픽 처리")
    void publishEvent_ShouldHandleEmptyTopic() throws Throwable {
        // given
        String emptyTopic = "";
        when(kafkaTemplate.send(emptyTopic, KEY, EVENT)).thenReturn(sendResultFuture);
        when(kafkaCircuitBreaker.executeCheckedSupplier(any())).thenAnswer(invocation -> {
            CheckedFunction0<?> supplier = invocation.getArgument(0);
            return supplier.apply();
        });

        // when
        assertDoesNotThrow(() -> resilientEventPublisher.publishEvent(emptyTopic, KEY, EVENT));

        // then
        verify(kafkaTemplate).send(emptyTopic, KEY, EVENT);
        assertThat(localBackupQueue).isEmpty();
    }
}
