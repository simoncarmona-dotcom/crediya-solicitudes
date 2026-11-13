package co.com.crediya.api;

import co.com.crediya.model.solicitud.Estado;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.TipoPrestamo;
import co.com.crediya.model.solicitud.exception.BusinessException;
import co.com.crediya.usecase.solicitud.SolicitudUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TestConfig.class)
class SolicitudApiRestTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SolicitudUseCase solicitudUseCase;

    private RestTestClient client;

    @BeforeEach
    void setup() {
        Mockito.reset(solicitudUseCase);
        client = RestTestClient.bindToApplicationContext(context).build();
    }

    @Test
    void shouldRegisterSolicitudAndReturn201Created() {
        Solicitud savedSolicitud = Solicitud.builder()
                .idSolicitud(1L)
                .documentoIdentidad("1020304050")
                .email("cliente@crediya.com")
                .monto(new BigDecimal("5000000.00"))
                .plazo(24)
                .idTipoPrestamo(1L)
                .tipoPrestamo(TipoPrestamo.builder().idTipoPrestamo(1L).nombre("Personal").build())
                .idEstado(1L)
                .estado(Estado.builder().idEstado(1L).nombre("Pendiente de revisión").build())
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class))).thenReturn(savedSolicitud);

        String requestJson = """
                {
                  "documento_identidad": "1020304050",
                  "email": "cliente@crediya.com",
                  "monto": 5000000,
                  "plazo": 24,
                  "id_tipo_prestamo": 1
                }
                """;

        client.post()
                .uri("/api/v1/solicitud")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id_solicitud").isEqualTo(1)
                .jsonPath("$.documento_identidad").isEqualTo("1020304050")
                .jsonPath("$.estado").isEqualTo("Pendiente de revisión")
                .jsonPath("$.tipo_prestamo").isEqualTo("Personal");
    }

    @Test
    void shouldReturn400WhenValidationFails() {
        String invalidJson = """
                {
                  "documento_identidad": "",
                  "monto": -100,
                  "plazo": 0
                }
                """;

        client.post()
                .uri("/api/v1/solicitud")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void shouldReturn400WhenTipoPrestamoDoesNotExist() {
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class)))
                .thenThrow(new BusinessException("El tipo de préstamo seleccionado no existe: 99"));

        String requestJson = """
                {
                  "documento_identidad": "1020304050",
                  "monto": 5000000,
                  "plazo": 12,
                  "id_tipo_prestamo": 99
                }
                """;

        client.post()
                .uri("/api/v1/solicitud")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo("El tipo de préstamo seleccionado no existe: 99");
    }
}
