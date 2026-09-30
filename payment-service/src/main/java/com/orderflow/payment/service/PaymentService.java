package com.orderflow.payment.service;

import com.orderflow.payment.dto.PaymentCompensationEventDto;
import com.orderflow.payment.dto.PaymentEventDto;
import com.orderflow.payment.dto.PaymentResultEventDto;
import com.orderflow.payment.messaging.PaymentPublisher;
import com.orderflow.payment.repository.ProcessedPaymentEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

  private final PaymentPublisher paymentPublisher;
  private final ProcessedPaymentEventRepository processedPaymentEventRepository;

  public void processPayment(UUID eventId, Long orderId, Long productId, Integer quantity, String email) {

    // Idempotência: INSERT ... ON CONFLICT (event_id) DO NOTHING.
    // Nunca existsBy + save (UnexpectedRollbackException sob concorrência).
    int inserted = processedPaymentEventRepository.insertIfNotExists(eventId);
    if (inserted == 0) {
      log.info("Event already processed, ignoring. eventId={}", eventId);
      return;
    }

    boolean approved = authorize(orderId, productId, quantity);

    if (approved) {
      paymentPublisher.publishPaymentResult(
              new PaymentResultEventDto(orderId, true, email, eventId)
      );
      return;
    }

    // Caminho de recusa: resultado negado + compensação para devolver o estoque
    // reservado pelo inventory. A lógica de decisão (authorize) é determinística
    // nesta versão e sempre aprova; este ramo fica pronto para a regra real.
    log.warn("Payment refused for orderId={}; publishing rejection and compensation", orderId);
    paymentPublisher.publishPaymentResult(
            new PaymentResultEventDto(orderId, false, email, eventId)
    );
    paymentPublisher.publishPaymentCompensation(
            new PaymentCompensationEventDto(orderId, productId, quantity, eventId)
    );
  }

  /**
   * Decide se o pagamento é aprovado.
   *
   * <p>Versão atual: sempre aprova (determinística, para teste manual).
   * A lógica de recusa será implementada em uma próxima parte.
   *
   * <p>Visibilidade {@code protected} (era {@code private}) para permitir que
   * os testes exercitem o caminho de recusa/compensação via override —
   * sem alterar o comportamento de produção.
   */
  protected boolean authorize(Long orderId, Long productId, Integer quantity) {
    log.info("Authorizing payment for orderId={} productId={} quantity={}: approved",
            orderId, productId, quantity);
    return true;
  }
}
