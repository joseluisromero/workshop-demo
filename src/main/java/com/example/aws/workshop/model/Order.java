package com.example.aws.workshop.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders", schema = "workshop")
public class Order {
    
    @Id
    @Column("id")
    private UUID id;
    
    @Column("email")
    private String email;
    
    @Column("products")
    private String products;
    
    @Column("created_at")
    private LocalDateTime createdAt;
}
