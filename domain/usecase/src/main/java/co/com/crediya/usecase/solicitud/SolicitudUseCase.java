package co.com.crediya.usecase.solicitud;

import co.com.crediya.model.solicitud.Estado;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.TipoPrestamo;
import co.com.crediya.model.solicitud.gateways.EstadoRepository;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.crediya.model.solicitud.exception.BusinessException;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SolicitudUseCase {

    private static final Logger LOG = Logger.getLogger(SolicitudUseCase.class.getName());
    private static final String ESTADO_INICIAL_NOMBRE = "Pendiente de revisión";

    private final SolicitudRepository solicitudRepository;
    private final TipoPrestamoRepository tipoPrestamoRepository;
    private final EstadoRepository estadoRepository;

    public Solicitud registrarSolicitud(Solicitud solicitud) {
        if (solicitud == null) {
            throw new BusinessException("La información de la solicitud es obligatoria.");
        }

        LOG.info("Ejecutando validaciones de negocio para la solicitud de crédito");
        validarDatosBasicos(solicitud);

        TipoPrestamo tipoPrestamo = tipoPrestamoRepository.findById(solicitud.getIdTipoPrestamo());
        if (tipoPrestamo == null) {
            throw new BusinessException("El tipo de préstamo seleccionado no existe: " + solicitud.getIdTipoPrestamo());
        }

        validarMontoSegunTipoPrestamo(solicitud.getMonto(), tipoPrestamo);

        Estado estadoInicial = estadoRepository.findByNombre(ESTADO_INICIAL_NOMBRE)
                .orElseGet(() -> {
                    Estado e = estadoRepository.findById(1L);
                    return e != null ? e : Estado.builder()
                            .idEstado(1L)
                            .nombre(ESTADO_INICIAL_NOMBRE)
                            .descripcion("Solicitud registrada pendiente de evaluación")
                            .build();
                });

        solicitud.setTipoPrestamo(tipoPrestamo);
        solicitud.setIdTipoPrestamo(tipoPrestamo.getIdTipoPrestamo());
        solicitud.setEstado(estadoInicial);
        solicitud.setIdEstado(estadoInicial.getIdEstado());
        if (solicitud.getFechaCreacion() == null) {
            solicitud.setFechaCreacion(LocalDateTime.now());
        }

        LOG.info("Guardando solicitud de crédito con estado inicial: " + estadoInicial.getNombre());
        return solicitudRepository.save(solicitud);
    }

    private void validarDatosBasicos(Solicitud solicitud) {
        if (solicitud.getDocumentoIdentidad() == null || solicitud.getDocumentoIdentidad().isBlank()) {
            throw new BusinessException("El campo 'documento_identidad' es obligatorio y no puede estar vacío.");
        }
        if (solicitud.getMonto() == null) {
            throw new BusinessException("El campo 'monto' es obligatorio.");
        }
        if (solicitud.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto del préstamo debe ser mayor a cero.");
        }
        if (solicitud.getPlazo() == null) {
            throw new BusinessException("El campo 'plazo' es obligatorio.");
        }
        if (solicitud.getPlazo() <= 0) {
            throw new BusinessException("El plazo en meses debe ser mayor a cero.");
        }
        if (solicitud.getIdTipoPrestamo() == null) {
            throw new BusinessException("El campo 'id_tipo_prestamo' es obligatorio.");
        }
    }

    private void validarMontoSegunTipoPrestamo(BigDecimal monto, TipoPrestamo tipoPrestamo) {
        if (tipoPrestamo.getMontoMinimo() != null && monto.compareTo(tipoPrestamo.getMontoMinimo()) < 0) {
            throw new BusinessException("El monto solicitado ($" + monto + ") es menor al monto mínimo permitido ($"
                    + tipoPrestamo.getMontoMinimo() + ") para el tipo de préstamo '" + tipoPrestamo.getNombre() + "'.");
        }
        if (tipoPrestamo.getMontoMaximo() != null && monto.compareTo(tipoPrestamo.getMontoMaximo()) > 0) {
            throw new BusinessException("El monto solicitado ($" + monto + ") supera el monto máximo permitido ($"
                    + tipoPrestamo.getMontoMaximo() + ") para el tipo de préstamo '" + tipoPrestamo.getNombre() + "'.");
        }
    }
}
