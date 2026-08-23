package com.clientesinternet.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "configuraciones")
@Data
@NoArgsConstructor
public class Configuracion {
    
    @Id
    private String clave; 
    private Double valor;
    private String valorString;

    // Constructor para mantener compatibles tus precios de TV
    public Configuracion(String clave, Double valor) {
        this.clave = clave;
        this.valor = valor;
    }

    // Constructor nuevo para guardar textos (como el email)
    public Configuracion(String clave, String valorString) {
        this.clave = clave;
        this.valorString = valorString;
    }
}