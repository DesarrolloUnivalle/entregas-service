package com.tienda.entregas.kafka;

import com.tienda.entregas.dto.EntregaAsignadaEvent;
import com.tienda.entregas.dto.PedidoCreadoEvent;
import com.tienda.entregas.kafka.consumer.PedidoConsumer;
import com.tienda.entregas.kafka.producer.EntregaProducer;
import com.tienda.entregas.model.entity.Entrega;
import com.tienda.entregas.model.entity.Entrega.EntregaStatus;
import com.tienda.entregas.service.EntregaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class KafkaComponentsTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private EntregaService entregaService;

    @InjectMocks
    private EntregaProducer entregaProducer;

    private PedidoConsumer pedidoConsumer;

    @Captor
    private ArgumentCaptor<EntregaAsignadaEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pedidoConsumer = new PedidoConsumer(entregaService);
    }

    @Test
    void publicarEventoEntregaAsignada_deberiaEnviarMensajeKafka() {
        Entrega entrega = crearEntrega();

        entregaProducer.publicarEventoEntregaAsignada(entrega);

        verify(kafkaTemplate).send(eq("entregas-asignadas"), eventCaptor.capture());
        EntregaAsignadaEvent evento = eventCaptor.getValue();

        assertEquals(entrega.getOrdenId(), evento.getOrdenId());
        assertEquals(entrega.getRepartidorId(), evento.getRepartidorId());
        assertEquals(entrega.getEstado().name(), evento.getEstado());
        assertEquals(entrega.getFechaAsignacion(), evento.getFechaAsignacion());
    }

    @Test
    void publicarEventoEntregaCompletada_deberiaEnviarMensajeKafka() {
        Entrega entrega = crearEntrega();

        entregaProducer.publicarEventoEntregaCompletada(entrega);

        verify(kafkaTemplate).send(eq("entregas-completadas"), eventCaptor.capture());
        EntregaAsignadaEvent evento = eventCaptor.getValue();

        assertEquals(entrega.getOrdenId(), evento.getOrdenId());
        assertEquals(entrega.getRepartidorId(), evento.getRepartidorId());
        assertEquals(entrega.getEstado().getValor(), evento.getEstado());

    }

    @Test
    void escucharPedidoCreado_deberiaLlamarAsignarRepartidor() {
        PedidoCreadoEvent evento = new PedidoCreadoEvent();
        evento.setOrdenId(123L);
        evento.setDireccionEntrega("Cra 8 # 20-15");

        pedidoConsumer.escucharPedidoCreado(evento);

        verify(entregaService).asignarRepartidorAutomatico(123L, "Cra 8 # 20-15");
    }

    private Entrega crearEntrega() {
        Entrega entrega = new Entrega();
        entrega.setOrdenId(1L);
        entrega.setRepartidorId(2L);
        entrega.setEstado(EntregaStatus.Entregado);
        entrega.setFechaAsignacion(LocalDateTime.now());
        return entrega;
    }
}
