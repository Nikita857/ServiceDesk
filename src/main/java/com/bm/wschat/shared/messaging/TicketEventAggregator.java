package com.bm.wschat.shared.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Агрегатор событий тикетов для консолидации в рамках одного HTTP-запроса.
 * 
 * Проблема: при операциях вроде "взять тикет в работу" генерируется несколько
 * событий:
 * - STATUS_CHANGED
 * - ASSIGNED
 * - UPDATED
 * 
 * Это приводит к множественным WebSocket сообщениям и лишним ререндерам на
 * фронте.
 * 
 * Решение: собираем все события за транзакцию и отправляем только последнее
 * (наиболее актуальное) событие для каждого тикета.
 * 
 * Приоритет событий (от низкого к высокому):
 * UPDATED < STATUS_CHANGED < ASSIGNED < RATED < MESSAGE_SENT < CREATED <
 * DELETED
 */
@Slf4j
@Component
@RequestScope
public class TicketEventAggregator {

    /**
     * Карта: ticketId -> последнее событие для этого тикета.
     * LinkedHashMap сохраняет порядок добавления.
     */
    private final Map<Long, TicketEvent> pendingEvents = new LinkedHashMap<>();

    /**
     * Добавить событие в агрегатор.
     * Если для этого тикета уже есть событие — заменяем на более приоритетное.
     */
    public void addEvent(TicketEvent event) {
        Long ticketId = event.ticketId();

        TicketEvent existing = pendingEvents.get(ticketId);
        if (existing == null || shouldReplace(existing, event)) {
            pendingEvents.put(ticketId, event);
            log.trace("Event aggregated: ticketId={}, type={}", ticketId, event.type());
        } else {
            log.trace("Event skipped (lower priority): ticketId={}, type={}, existing={}",
                    ticketId, event.type(), existing.type());
        }
    }

    /**
     * Получить все агрегированные события.
     */
    public Collection<TicketEvent> getEvents() {
        return pendingEvents.values();
    }

    /**
     * Очистить агрегатор.
     */
    public void clear() {
        pendingEvents.clear();
    }

    /**
     * Есть ли ожидающие события?
     */
    public boolean hasEvents() {
        return !pendingEvents.isEmpty();
    }

    /**
     * Количество ожидающих событий.
     */
    public int size() {
        return pendingEvents.size();
    }

    /**
     * Определяет, нужно ли заменить существующее событие на новое.
     * Новое событие заменяет старое если у него выше или равный приоритет.
     */
    private boolean shouldReplace(TicketEvent existing, TicketEvent newEvent) {
        // DELETED всегда побеждает — тикет удалён, другие события не важны
        if (newEvent.type() == TicketEventType.DELETED) {
            return true;
        }
        // Если уже есть DELETED — ничего не заменяем
        if (existing.type() == TicketEventType.DELETED) {
            return false;
        }

        // Для остальных — новое событие всегда актуальнее
        // (оно содержит самый свежий payload)
        return true;
    }
}
