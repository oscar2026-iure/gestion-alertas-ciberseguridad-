package ec.monitoreo.alertas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(
        name = "relacion_alerta",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_relacion_alerta_origen_destino",
                    columnNames = {
                        "alerta_origen_id",
                        "alerta_relacionada_id"
                    }
            )
        }
)
public class RelacionAlerta {

    public static final int MAX_TIPO_RELACION = 30;
    public static final int MAX_MOTIVO = 500;
    public static final int MAX_ESTADO_VALIDACION = 20;
    public static final int MAX_OTRO_DETALLE = 100;

    private static final Set<String> TIPOS_PERMITIDOS =
            Set.of(
                    "POSIBLE_VINCULO",
                    "ANTECEDENTE",
                    "MISMA_CAMPANA",
                    "MISMO_INDICADOR",
                    "MISMO_ACTIVO",
                    "DUPLICADA",
                    "OTRA"
            );

    private static final Set<String> ESTADOS_PERMITIDOS =
            Set.of(
                    "PENDIENTE",
                    "EN_VALIDACION",
                    "CONFIRMADO",
                    "DESCARTADO"
            );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(
            name = "tipo_relacion",
            nullable = false,
            length = MAX_TIPO_RELACION
    )
    private String tipoRelacion;

    @Column(
            name = "motivo",
            nullable = false,
            length = MAX_MOTIVO
    )
    private String motivo;

    @Column(
            name = "estado_validacion",
            nullable = false,
            length = MAX_ESTADO_VALIDACION
    )
    private String estadoValidacion = "PENDIENTE";

    @Column(
            name = "fecha_registro",
            nullable = false,
            updatable = false
    )
    private LocalDateTime fechaRegistro;

    @Column(
            name = "otro_detalle",
            length = MAX_OTRO_DETALLE
    )
    private String otroDetalle;

    // Alerta desde la que se registra el vínculo.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "alerta_origen_id",
            nullable = false,
            updatable = false
    )
    private Alerta alertaOrigen;

    // Alerta seleccionada como relacionada.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "alerta_relacionada_id",
            nullable = false,
            updatable = false
    )
    private Alerta alertaRelacionada;

    // Constructor requerido por JPA.
    public RelacionAlerta() {
    }

    public RelacionAlerta(
            Alerta alertaOrigen,
            Alerta alertaRelacionada,
            String tipoRelacion,
            String motivo) {

        this.alertaOrigen = alertaOrigen;
        this.alertaRelacionada = alertaRelacionada;
        this.tipoRelacion = tipoRelacion;
        this.motivo = motivo;
        this.estadoValidacion = "PENDIENTE";
        this.fechaRegistro = LocalDateTime.now();
    }

    @PrePersist
    private void antesDeRegistrar() {

        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }

        validarDatos();
    }

    @PreUpdate
    private void antesDeActualizar() {
        validarDatos();
    }

    public void validarDatos() {

        validarTexto(
                "Tipo de relación",
                tipoRelacion,
                MAX_TIPO_RELACION,
                true
        );

        validarTexto(
                "Motivo",
                motivo,
                MAX_MOTIVO,
                true
        );

        validarTexto(
                "Estado de validación",
                estadoValidacion,
                MAX_ESTADO_VALIDACION,
                true
        );

        validarTexto(
                "Otro detalle",
                otroDetalle,
                MAX_OTRO_DETALLE,
                false
        );

        if (!TIPOS_PERMITIDOS.contains(tipoRelacion)) {
            throw new IllegalArgumentException(
                    "Seleccione un tipo de relación válido."
            );
        }

        if (!ESTADOS_PERMITIDOS.contains(estadoValidacion)) {
            throw new IllegalArgumentException(
                    "Seleccione un estado de validación válido."
            );
        }

        if (fechaRegistro == null) {
            throw new IllegalArgumentException(
                    "La fecha de registro es obligatoria."
            );
        }

        if (alertaOrigen == null || alertaRelacionada == null) {
            throw new IllegalArgumentException(
                    "Seleccione la alerta de origen "
                    + "y la alerta relacionada."
            );
        }

        /*
         * Para este flujo, primero deben existir las alertas
         * y después se registra la relación.
         */
        Long origenId = alertaOrigen.getId();
        Long relacionadaId = alertaRelacionada.getId();

        if (origenId == null || relacionadaId == null) {
            throw new IllegalArgumentException(
                    "Guarde ambas alertas antes de vincularlas."
            );
        }

        if (origenId.equals(relacionadaId)) {
            throw new IllegalArgumentException(
                    "Una alerta no puede relacionarse consigo misma."
            );
        }
    }

    private static void validarTexto(
            String campo,
            String valor,
            int maximo,
            boolean obligatorio) {

        if (obligatorio
                && (valor == null || valor.isBlank())) {

            throw new IllegalArgumentException(
                    campo + " es obligatorio."
            );
        }

        if (valor != null && valor.length() > maximo) {
            throw new IllegalArgumentException(
                    campo + " admite hasta "
                    + maximo + " caracteres."
            );
        }
    }

    public Long getId() {
        return id;
    }

    public String getTipoRelacion() {
        return tipoRelacion;
    }

    public void setTipoRelacion(String tipoRelacion) {
        this.tipoRelacion = tipoRelacion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getEstadoValidacion() {
        return estadoValidacion;
    }

    public void setEstadoValidacion(String estadoValidacion) {
        this.estadoValidacion = estadoValidacion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public String getOtroDetalle() {
        return otroDetalle;
    }

    public void setOtroDetalle(String otroDetalle) {
        this.otroDetalle = otroDetalle;
    }

    public Alerta getAlertaOrigen() {
        return alertaOrigen;
    }

    public void setAlertaOrigen(Alerta alertaOrigen) {

        if (id != null) {
            throw new IllegalStateException(
                    "No puede cambiar el origen de un vínculo guardado."
            );
        }

        this.alertaOrigen = alertaOrigen;
    }

    public Alerta getAlertaRelacionada() {
        return alertaRelacionada;
    }

    public void setAlertaRelacionada(Alerta alertaRelacionada) {

        if (id != null) {
            throw new IllegalStateException(
                    "No puede cambiar la alerta relacionada "
                    + "de un vínculo guardado."
            );
        }

        this.alertaRelacionada = alertaRelacionada;
    }
}