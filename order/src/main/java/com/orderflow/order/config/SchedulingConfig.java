package com.orderflow.order.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita o agendamento de tarefas (ex.: {@code OutboxPublisher.publishPendingEvents}).
 *
 * <p>Ligado por padrão em todos os ambientes ({@code matchIfMissing = true}); os
 * testes de integração o desligam com {@code order.scheduling.enabled=false} para
 * que o scheduler não dispare acessos ao datasource fora do controle do teste
 * (e sobre um container Testcontainers já encerrado).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "order.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
