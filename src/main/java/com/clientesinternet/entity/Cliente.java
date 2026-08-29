package com.clientesinternet.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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
    private String email; 
    private String sort;
    
    @Builder.Default
    private Integer mesesPagados = 0;

    @Column(name = "meses_adeudados_inicial")
    @Builder.Default
    private Integer mesesAdeudadosInicial = 0;
    
    @Builder.Default
    private Boolean tieneFibraTV = false;

    @Builder.Default
    private Boolean tieneTV = false;
    
    private String usuarioFibraTV;
    
    @Builder.Default
    private Boolean esDemo = false;
    
    private LocalDate fechaVencimientoDemo;
    
    @ManyToOne
    @JoinColumn(name = "plan_id")
    private PlanInternet plan;
    
    private String dni;
    
    @Builder.Default
    private String deudaInstalacion = "NO";

    @Builder.Default
    private Integer costoInstalacion = 0;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pago> pagos;

    @Transient
    @JsonProperty("mesesAdeudados")
    public int calcularMesesAdeudados() {
        // 1. Si el cliente tiene una deuda fija registrada o actualizada (Excel/Cobros parciales)
        if (this.mesesAdeudadosInicial != null) {
            return Math.max(0, this.mesesAdeudadosInicial);
        }

        // 2. Si no tiene deuda inicial fijada, se calcula mediante el historial de pagos de la BD
        YearMonth mesActual = YearMonth.now();
        if (pagos != null && !pagos.isEmpty()) {
            Pago ultimoPago = pagos.stream()
                    .filter(pago -> pago.getPeriodoPagado() != null)
                    .max(Comparator.comparing(Pago::getPeriodoPagado))
                    .orElse(null);

            if (ultimoPago != null) {
                YearMonth ultimoMesPagado = ultimoPago.getPeriodoPagado();
                if (!ultimoMesPagado.isBefore(mesActual)) {
                    return 0;
                }
                return (int) ChronoUnit.MONTHS.between(ultimoMesPagado, mesActual);
            }
        }

        if (this.mesesPagados != null && this.mesesPagados > 0) {
            return 0;
        }

        return 1;
    }
}