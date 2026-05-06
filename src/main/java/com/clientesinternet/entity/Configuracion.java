package com.clientesinternet.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "configuraciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Configuracion {
    @Id
    private String clave; // Ejemplo: "FIBRA_TV_PRECIO_EFECTIVO"
    private Double valor;
}