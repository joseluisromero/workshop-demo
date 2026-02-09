package com.example.aws.workshop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final SnsClient snsClient;

    @Value("${aws.sns.topic-arn}")
    private String topicArn;

    public void sendInvoiceNotification(String invoiceContent, String orderId) {
        try {
            log.info("Enviando notificación SNS para la orden: {}", orderId);
            
            String subject = String.format("Nueva Factura Generada - Orden %s", orderId);
            
            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject(subject)
                    .message(invoiceContent)
                    .build();
            
            PublishResponse response = snsClient.publish(publishRequest);
            
            log.info("Notificación SNS enviada exitosamente. MessageId: {}", response.messageId());
            
        } catch (Exception e) {
            log.error("Error al enviar notificación SNS para orden {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Error al enviar notificación SNS", e);
        }
    }
}
