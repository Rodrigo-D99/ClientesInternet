package com.clientesinternet.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "planes_internet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanInternet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer cantidadMB; 
    
    private Double precioEfectivo;
    private Double precioTransferencia;
}