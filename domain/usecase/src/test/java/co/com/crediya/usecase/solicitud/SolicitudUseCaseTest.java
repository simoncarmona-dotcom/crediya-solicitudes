package co.com.crediya.usecase.solicitud;

import co.com.crediya.model.solicitud.Estado;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.TipoPrestamo;
import co.com.crediya.model.solicitud.gateways.EstadoRepository;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.crediya.model.solicitud.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitudUseCaseTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private TipoPrestamoRepository tipoPrestamoRepository;

    @Mock
    private EstadoRepository estadoRepository;

    @InjectMocks
    private SolicitudUseCase solicitudUseCase;

    private TipoPrestamo tipoPersonal;
    private Estado estadoPendiente;
    private Solicitud solicitudValida;

    @BeforeEach
    void setUp() {
        tipoPersonal = TipoPrestamo.builder()
                .idTipoPrestamo(1L)
                .nombre("Personal")
                .montoMinimo(new BigDecimal("500000"))
                .montoMaximo(new BigDecimal("20000000"))
                .tasaInteres(new BigDecimal("0.02"))
                .validacionAutomatica(false)
                .build();

        estadoPendiente = Estado.builder()
                .idEstado(1L)
                .nombre("Pendiente de revisión")
                .descripcion("Solicitud pendiente de revisión")
                .build();

        solicitudValida = Solicitud.builder()
                .documentoIdentidad("1020304050")
                .email("cliente@crediya.com")
                .monto(new BigDecimal("5000000"))
                .plazo(24)
                .idTipoPrestamo(1L)
                .build();
    }

    @Test
    void shouldRegisterSolicitudWithEstadoPendienteDeRevision() {
        when(tipoPrestamoRepository.findById(1L)).thenReturn(tipoPersonal);
        when(estadoRepository.findByNombre("Pendiente de revisión")).thenReturn(Optional.of(estadoPendiente));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(invocation -> {
            Solicitud s = invocation.getArgument(0);
            s.setIdSolicitud(100L);
            return s;
        });

        Solicitud result = solicitudUseCase.registrarSolicitud(solicitudValida);

        assertNotNull(result);
        assertEquals(100L, result.getIdSolicitud());
        assertEquals("Pendiente de revisión", result.getEstado().getNombre());
        assertEquals(1L, result.getIdEstado());
        assertEquals("Personal", result.getTipoPrestamo().getNombre());
        assertNotNull(result.getFechaCreacion());
        verify(solicitudRepository).save(any(Solicitud.class));
    }

    @Test
    void shouldThrowExceptionWhenSolicitudIsNull() {
        assertThrows(BusinessException.class, () -> solicitudUseCase.registrarSolicitud(null));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDocumentoIdentidadIsBlank() {
        solicitudValida.setDocumentoIdentidad("   ");
        assertThrows(BusinessException.class, () -> solicitudUseCase.registrarSolicitud(solicitudValida));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenMontoIsZeroOrNegative() {
        solicitudValida.setMonto(BigDecimal.ZERO);
        assertThrows(BusinessException.class, () -> solicitudUseCase.registrarSolicitud(solicitudValida));

        solicitudValida.setMonto(new BigDecimal("-100"));
        assertThrows(BusinessException.class, () -> solicitudUseCase.registrarSolicitud(solicitudValida));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenPlazoIsZeroOrNegative() {
        solicitudValida.setPlazo(0);
        assertThrows(BusinessException.class, () -> solicitudUseCase.registrarSolicitud(solicitudValida));

        solicitudValida.setPlazo(-5);
        assertThrows(BusinessException.class, () -> solicitudUseCase.registrarSolicitud(solicitudValida));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenIdTipoPrestamoIsNull() {
        solicitudValida.setIdTipoPrestamo(null);
        assertThrows(BusinessException.class, () -> solicitudUseCase.registrarSolicitud(solicitudValida));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenTipoPrestamoDoesNotExist() {
        when(tipoPrestamoRepository.findById(99L)).thenReturn(null);
        solicitudValida.setIdTipoPrestamo(99L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> solicitudUseCase.registrarSolicitud(solicitudValida));

        assertTrue(exception.getMessage().contains("no existe"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenMontoBelowMinimum() {
        when(tipoPrestamoRepository.findById(1L)).thenReturn(tipoPersonal);
        solicitudValida.setMonto(new BigDecimal("100000")); // Min is 500000

        BusinessException exception = assertThrows(BusinessException.class,
                () -> solicitudUseCase.registrarSolicitud(solicitudValida));

        assertTrue(exception.getMessage().contains("menor al monto mínimo"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenMontoAboveMaximum() {
        when(tipoPrestamoRepository.findById(1L)).thenReturn(tipoPersonal);
        solicitudValida.setMonto(new BigDecimal("25000000")); // Max is 20000000

        BusinessException exception = assertThrows(BusinessException.class,
                () -> solicitudUseCase.registrarSolicitud(solicitudValida));

        assertTrue(exception.getMessage().contains("supera el monto máximo"));
        verify(solicitudRepository, never()).save(any());
    }
}
