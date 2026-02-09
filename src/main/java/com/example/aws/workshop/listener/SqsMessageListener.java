package com.example.aws.workshop.listener;

import com.example.aws.workshop.dto.OrderMessage;
import com.example.aws.workshop.model.Order;
import com.example.aws.workshop.repository.OrderRepository;
import com.example.aws.workshop.service.InvoiceService;
import com.example.aws.workshop.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsMessageListener {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final InvoiceService invoiceService;
    private final NotificationService notificationService;

    @SqsListener("${aws.sqs.queue-name}")
    public void receiveMessage(Message<String> message) {
        String messageBody = message.getPayload();
        
        log.info("========================================");
        log.info("Mensaje recibido de SQS:");
        log.info("Contenido: {}", messageBody);
        log.info("========================================");
        
        try {
            // Parsear el mensaje JSON
            OrderMessage orderMessage = objectMapper.readValue(messageBody, OrderMessage.class);
            
            // Validar campos obligatorios
            if (orderMessage.getEmail() == null || orderMessage.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("El campo 'email' es obligatorio");
            }
            
            if (orderMessage.getProducts() == null || orderMessage.getProducts().trim().isEmpty()) {
                throw new IllegalArgumentException("El campo 'products' es obligatorio");
            }
            
            // Crear la entidad Order
            Order order = Order.builder()
                    .email(orderMessage.getEmail())
                    .products(orderMessage.getProducts())
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // Guardar en la base de datos
            Order savedOrder = orderRepository.save(order);
            
            log.info("Orden guardada exitosamente con ID: {}", savedOrder.getId());
            System.out.println("\n>>> ORDEN GUARDADA EN BD <<<");
            System.out.println("ID: " + savedOrder.getId());
            System.out.println("Email: " + savedOrder.getEmail());
            System.out.println("Products: " + savedOrder.getProducts());
            System.out.println("Created At: " + savedOrder.getCreatedAt());
            System.out.println(">>> FIN <<<\n");
            
            // Generar y subir factura a S3
            String invoiceContent = invoiceService.generateAndUploadInvoice(savedOrder);
            log.info("Factura generada y subida a S3 exitosamente");
            
            // Enviar notificación SNS con el contenido de la factura
            notificationService.sendInvoiceNotification(invoiceContent, savedOrder.getId().toString());
            log.info("Notificación SNS enviada exitosamente");
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            System.err.println("\n>>> ERROR DE VALIDACIÓN <<<");
            System.err.println(e.getMessage());
            System.err.println(">>> FIN <<<\n");
            throw e;
        } catch (Exception e) {
            log.error("Error al procesar el mensaje: {}", e.getMessage(), e);
            System.err.println("\n>>> ERROR AL PROCESAR MENSAJE <<<");
            System.err.println(e.getMessage());
            System.err.println(">>> FIN <<<\n");
            throw new RuntimeException("Error al procesar el mensaje", e);
        }
    }
}
