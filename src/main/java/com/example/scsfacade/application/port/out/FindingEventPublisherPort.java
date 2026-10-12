package com.example.scsfacade.application.port.out;

import java.util.List;

/**
 * 弱點事件發布外向埠 (FindingEventPublisherPort)。
 * <p>
 * 遵循 ... + Port 命名規範。應用層要求基礎設施廣播發布領域事件（例如 Spring Event、MQ 等）之介面。
 * </p>
 */
public interface FindingEventPublisherPort {

    /**
     * 發布單一領域事件。
     *
     * @param event 領域事件物件
     */
    void publish(Object event);

    /**
     * 批次發布領域事件清單。
     *
     * @param events 領域事件清單
     */
    void publishAll(List<Object> events);
}
