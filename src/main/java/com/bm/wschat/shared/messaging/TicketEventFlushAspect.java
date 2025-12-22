package com.bm.wschat.shared.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AOP аспект для автоматической отправки агрегированных событий после
 * транзакции.
 * 
 * Перехватывает все @Transactional методы в Service слое и вызывает flush()
 * после успешного завершения метода.
 * 
 * Order(-1) — выполняется ПОСЛЕ транзакционного аспекта (который имеет
 * Order(0)).
 */
@Slf4j
@Aspect
@Component
@Order(-1) // Выполняется после @Transactional аспекта
@RequiredArgsConstructor
public class TicketEventFlushAspect {

    private final TicketEventPublisher eventPublisher;

    /**
     * Перехватывает @Transactional методы и вызывает flush после успешного
     * выполнения.
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional) && " +
            "(within(com.bm.wschat.feature.ticket.service.*) || " +
            " within(com.bm.wschat.feature.message..*) || " +
            " within(com.bm.wschat.feature.attachment.service.*))")
    public Object flushAfterTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object result = joinPoint.proceed();

            // После успешного выполнения — отправляем накопленные события
            eventPublisher.flush();

            return result;
        } catch (Throwable t) {
            // При ошибке events не отправляем (транзакция откатится)
            log.debug("Transaction failed, events not flushed: {}", t.getMessage());
            throw t;
        }
    }
}
