package com.mindmate.mindmate_server.global.service;

import com.mindmate.mindmate_server.global.dto.EventWrapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CircuitBreaker kafkaCircuitBreaker;
    private final BlockingQueue<EventWrapper<?>> localBackupQueue = new LinkedBlockingQueue<>(10000);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void startBackupProcessor() {
        scheduler.scheduleWithFixedDelay(this::processBackupQueue, 0, 1, TimeUnit.MINUTES);
    }

    public <T> void publishEvent(String topic, String key, T event) {
        try {
            // 서킷 브레이커 적용
            kafkaCircuitBreaker.executeCheckedSupplier(() -> {
                kafkaTemplate.send(topic, key, event).get(500, TimeUnit.MILLISECONDS);
                return null;
            });
        } catch (Exception e) {
            log.warn("카프카 발행 실패, 로컬 큐에 저장: {}", e.getMessage());
            boolean offered = localBackupQueue.offer(new EventWrapper<>(topic, key, event));
            if (!offered) {
                log.error("로컬 백업 큐 full: {}", event);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void publishEvent(String topic, T event) {
        publishEvent(topic, null ,event);
    }

    private void processBackupQueue() {
        int  count = 0;
        while (!localBackupQueue.isEmpty() && count < 100) {
            EventWrapper<?> wrapper = localBackupQueue.peek();
            try {
                if (wrapper.getKey() != null) {
                    kafkaTemplate.send(wrapper.getTopic(), wrapper.getKey(), wrapper.getEvent()).get(500, TimeUnit.MILLISECONDS);
                } else {
                    kafkaTemplate.send(wrapper.getTopic(), wrapper.getEvent()).get(500, TimeUnit.MILLISECONDS);
                }
                localBackupQueue.poll();
                count++;
            } catch (Exception e) {
                log.warn("백업 큐 처리 실패, 다음 스케줄에 재시도: {}", e.getMessage());
                break;
            }
        }

        if (count > 0) {
            log.info("백업 큐에서 {} 개의 이벤트 재발행 완료", count);
        }
    }
}
