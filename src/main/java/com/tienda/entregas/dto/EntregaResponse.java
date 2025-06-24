package com.tienda.entregas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntregaResponse {
    private Long id;
    private Long ordenId;
    private Long pedidoId;
    private Long repartidorId;
    private String estado;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaEntrega;
    private String direccionEntrega;
}