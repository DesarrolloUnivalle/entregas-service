package integration;

import com.tienda.entregas.EntregasApplication;
import com.tienda.entregas.dto.PedidoCreadoEvent;
import com.tienda.entregas.model.entity.Entrega;
import com.tienda.entregas.repository.EntregaRepository;
import com.tienda.entregas.service.impl.EntregaServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@SpringBootTest(classes = EntregasApplication.class)
@ActiveProfiles("test")
@DirtiesContext
public class KafkaEventListenerIntegrationTest {

    @Autowired
    private EntregaServiceImpl entregaService;

    @Autowired
    private EntregaRepository entregaRepository;

    @Test
    @Transactional
    public void testProcesamientoDeEventoPedidoCreado() {
        // Crear un evento simulado
        PedidoCreadoEvent event = new PedidoCreadoEvent();
        event.setOrdenId(99L);
        event.setDireccionEntrega("Calle falsa 123");
        event.setFechaCreacion(LocalDateTime.now());

        // Simular la recepción del evento como lo haría el listener
        entregaService.asignarRepartidorAutomatico(event.getOrdenId(), event.getDireccionEntrega());

        // Verificar que la entrega fue creada en la base de datos
        Optional<Entrega> entregaOptional = entregaRepository.findByOrdenId(99L).stream().findFirst();
        Assertions.assertTrue(entregaOptional.isPresent(), "La entrega no fue creada");

        Entrega entrega = entregaOptional.get();
        Assertions.assertEquals("Calle falsa 123", entrega.getDireccionEntrega());
        Assertions.assertEquals(99L, entrega.getOrdenId());
    }
}
