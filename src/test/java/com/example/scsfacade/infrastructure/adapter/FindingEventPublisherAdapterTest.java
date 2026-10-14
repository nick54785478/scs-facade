package com.example.scsfacade.infrastructure.adapter;

import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionId;
import com.example.scsfacade.domain.scanexecution.event.ScanExecutionStartedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 弱點事件發布外向適配器單元測試 (FindingEventPublisherAdapterTest)。
 * <p>
 * 驗證 FindingEventPublisherAdapter 透過 ApplicationEventPublisher 轉發領域事件。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class FindingEventPublisherAdapterTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private FindingEventPublisherAdapter adapter;

    /**
     * Feature: 發布單一領域事件
     * Scenario: 傳入事件物件正確轉交 publishEvent
     */
    @Test
    @DisplayName("發布單一事件 - 應轉交 ApplicationEventPublisher")
    void shouldPublishSingleEvent() {
        // Given
        DomainEvent event = new ScanExecutionStartedEvent(
                ScanExecutionId.of("e-1"), "p-1", "main", "c-1", LocalDateTime.now()
        );

        // When
        adapter.publish(event);

        // Then
        verify(applicationEventPublisher).publishEvent(event);
    }

    /**
     * Feature: 發布多個領域事件
     * Scenario: 傳入事件清單應依序轉交
     */
    @Test
    @DisplayName("發布事件清單 - 應依序發布各事件")
    void shouldPublishAllEvents() {
        // Given
        DomainEvent event1 = new ScanExecutionStartedEvent(
                ScanExecutionId.of("e-1"), "p-1", "main", "c-1", LocalDateTime.now()
        );
        DomainEvent event2 = new ScanExecutionStartedEvent(
                ScanExecutionId.of("e-2"), "p-1", "main", "c-2", LocalDateTime.now()
        );

        // When
        adapter.publishAll(List.of(event1, event2));

        // Then
        verify(applicationEventPublisher).publishEvent(event1);
        verify(applicationEventPublisher).publishEvent(event2);
    }

    /**
     * Feature: null 事件防禦
     * Scenario: 傳入 null 時不觸發任何操作
     */
    @Test
    @DisplayName("空值防禦 - 傳入 null 事件不應觸發 publisher")
    void shouldDoNothingWhenEventIsNull() {
        // When
        adapter.publish(null);
        adapter.publishAll(null);

        // Then
        verifyNoInteractions(applicationEventPublisher);
    }
}
