package com.example.aws.workshop.service;

import com.example.aws.workshop.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.invoice-prefix}")
    private String invoicePrefix;

    public String generateAndUploadInvoice(Order order) {
        try {
            log.info("Generando factura para la orden ID: {}", order.getId());
            
            // Generar el contenido de la factura
            String invoiceContent = generateInvoiceContent(order);
            log.info("Contenido de la factura generado:\n{}", invoiceContent);
            
            // Generar el nombre del archivo
            String fileName = String.format("%s_%s_%s.txt", 
                    invoicePrefix,
                    order.getId().toString(), 
                    order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            log.info("Nombre del archivo de la factura: {}", fileName);
            
            // Subir archivo a S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType("text/plain")
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromString(invoiceContent));
            
            log.info("Factura subida exitosamente a S3: s3://{}/{}", bucketName, fileName);
            
            // Retornar el contenido de la factura para enviarlo por SNS
            return invoiceContent;
            
        } catch (Exception e) {
            log.error("Error al generar/subir factura para orden {}: {}", order.getId(), e.getMessage(), e);
            throw new RuntimeException("Error al procesar la factura", e);
        }
    }

    private String generateInvoiceContent(Order order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        
        StringBuilder invoice = new StringBuilder();
        invoice.append("========================================\n");
        invoice.append("           FACTURA DE COMPRA            \n");
        invoice.append("========================================\n\n");
        invoice.append("ID Orden: ").append(order.getId()).append("\n");
        invoice.append("Fecha: ").append(order.getCreatedAt().format(formatter)).append("\n");
        invoice.append("Cliente: ").append(order.getEmail()).append("\n");
        invoice.append("\n========================================\n");
        invoice.append("              PRODUCTOS                 \n");
        invoice.append("========================================\n\n");
        invoice.append(order.getProducts()).append("\n");
        invoice.append("\n========================================\n");
        invoice.append("   Gracias por su compra!              \n");
        invoice.append("========================================\n");
        
        return invoice.toString();
    }
}
