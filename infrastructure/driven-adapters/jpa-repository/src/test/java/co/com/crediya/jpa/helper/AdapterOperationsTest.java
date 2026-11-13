package co.com.crediya.jpa.helper;

import co.com.crediya.jpa.SolicitudJPARepository;
import co.com.crediya.jpa.SolicitudRepositoryAdapter;
import co.com.crediya.jpa.entity.SolicitudEntity;
import co.com.crediya.model.solicitud.Solicitud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.reactivecommons.utils.ObjectMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class AdapterOperationsTest {

    @Mock
    private SolicitudJPARepository repository;

    @Mock
    private ObjectMapper objectMapper;

    private SolicitudRepositoryAdapter adapter;
    private Solicitud domainSolicitud;
    private SolicitudEntity entitySolicitud;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        domainSolicitud = Solicitud.builder()
                .idSolicitud(1L)
                .documentoIdentidad("123456789")
                .monto(new BigDecimal("5000000"))
                .plazo(12)
                .idTipoPrestamo(1L)
                .idEstado(1L)
                .build();

        entitySolicitud = SolicitudEntity.builder()
                .idSolicitud(1L)
                .documentoIdentidad("123456789")
                .monto(new BigDecimal("5000000"))
                .plazo(12)
                .idTipoPrestamo(1L)
                .idEstado(1L)
                .build();

        when(objectMapper.map(domainSolicitud, SolicitudEntity.class)).thenReturn(entitySolicitud);
        when(objectMapper.map(entitySolicitud, Solicitud.class)).thenReturn(domainSolicitud);

        adapter = new SolicitudRepositoryAdapter(repository, objectMapper);
    }

    @Test
    void testSave() {
        when(repository.save(entitySolicitud)).thenReturn(entitySolicitud);

        Solicitud result = adapter.save(domainSolicitud);
        assertNotNull(result);
        assertEquals(domainSolicitud.getDocumentoIdentidad(), result.getDocumentoIdentidad());
    }

    @Test
    void testFindById() {
        when(repository.findById(1L)).thenReturn(Optional.of(entitySolicitud));

        Solicitud result = adapter.findById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getIdSolicitud());
    }
}
