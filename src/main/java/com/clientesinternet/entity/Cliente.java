package com.clientesinternet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String telefono;

    private String direccion;
    private String sort;
    
    @Builder.Default
    private Integer mesesPagados = 0;
    
    @Builder.Default
    private Boolean tieneFibraTV = false;
    
    private String usuarioFibraTV;
    
    @Builder.Default
    private Boolean esDemo = false;
    
    private LocalDate fechaVencimientoDemo;
    
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pago> pagos;
}
