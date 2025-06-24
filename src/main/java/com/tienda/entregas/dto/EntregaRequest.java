package com.tienda.entregas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntregaRequest {

    @NotNull(message = "El ID de la orden es obligatorio")
    private Long ordenId;

    @NotNull(message = "El ID del repartidor es obligatorio")
    private Long repartidorId;

    @NotBlank(message = "La dirección de entrega no puede estar vacía")
    private String direccionEntrega;
}
