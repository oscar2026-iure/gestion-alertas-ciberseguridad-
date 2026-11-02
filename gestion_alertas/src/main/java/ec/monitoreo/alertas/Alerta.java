package ec.monitoreo.alertas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "alerta")
public class Alerta {

    // Límites compartidos con la interfaz gráfica.
    public static final int MAX_DESCRIPCION = 1000;
    public static final int MAX_SEVERIDAD = 20;
    public static final int MAX_RESPONSABLE = 150;
    public static final int MAX_ESTADO = 30;
    public static final int MAX_MOTIVO_ANULACION = 500;

    private static final Set<String> SEVERIDADES_PERMITIDAS =
            Set.of(
                    "BAJA",
                    "MEDIA",
                    "ALTA",
                    "CRITICA"
            );

    private static final Set<String> ESTADOS_PERMITIDOS =
            Set.of(
                    "REGISTRADA",
                    "EN_ANALISIS",
                    "EN_TRATAMIENTO",
                    "CERRADA"
            );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(
            name = "descripcion",
            nullable = false,
            length = MAX_DESCRIPCION
    )
    private String descripcion;

    @Column(
            name = "severidad",
            nullable = false,
            length = MAX_SEVERIDAD
    )
    private String severidad;

    @Column(
            name = "responsable",
            nullable = false,
            length = MAX_RESPONSABLE
    )
    private String responsable;

    @Column(
            name = "estado",
            nullable = false,
            length = MAX_ESTADO
    )
    private String estado = "REGISTRADA";

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(
            name = "motivo_anulacion",
            length = MAX_MOTIVO_ANULACION
    )
    private String motivoAnulacion;

    // Constructor sin parámetros requerido por JPA.
    public Alerta() {
    }

    // Compatible con el registro de tu pantalla actual.
    public Alerta(
            String descripcion,
            String severidad,
            String responsable) {

        this.fecha = LocalDateTime.now();
        this.descripcion = descripcion;
        this.severidad = severidad;
        this.responsable = responsable;
        this.estado = "REGISTRADA";
        this.activo = true;
    }

    // Se ejecuta antes de insertar una alerta.
    @PrePersist
    private void antesDeRegistrar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }

        validarDatos();
    }

    // Se ejecuta antes de actualizar una alerta.
    @PreUpdate
    private void antesDeActualizar() {
        validarDatos();
    }

    // También puede llamarse desde la lógica de la aplicación.
    public void validarDatos() {

        validarTexto(
                "Descripción",
                descripcion,
                MAX_DESCRIPCION,
                true
        );

        validarTexto(
                "Severidad",
                severidad,
                MAX_SEVERIDAD,
                true
        );

        validarTexto(
                "Responsable",
                responsable,
                MAX_RESPONSABLE,
                true
        );

        validarTexto(
                "Estado",
                estado,
                MAX_ESTADO,
                true
        );

        validarTexto(
                "Motivo de anulación",
                motivoAnulacion,
                MAX_MOTIVO_ANULACION,
                !activo
        );

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La fecha de registro es obligatoria."
            );
        }

        if (!SEVERIDADES_PERMITIDAS.contains(severidad)) {
            throw new IllegalArgumentException(
                    "Seleccione una severidad válida: "
                    + "BAJA, MEDIA, ALTA o CRITICA."
            );
        }

        if (!ESTADOS_PERMITIDOS.contains(estado)) {
            throw new IllegalArgumentException(
                    "Seleccione un estado válido: REGISTRADA, "
                    + "EN_ANALISIS, EN_TRATAMIENTO o CERRADA."
            );
        }

        if (activo
                && motivoAnulacion != null
                && !motivoAnulacion.isBlank()) {

            throw new IllegalArgumentException(
                    "Una alerta vigente no debe tener "
                    + "motivo de anulación."
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

    public LocalDateTime getFecha() {
        return fecha;
    }

    // Se conserva por compatibilidad.
    // La fecha deja de ser modificable al tener un ID.
    public void setFecha(LocalDateTime fecha) {
        if (id != null) {
            throw new IllegalStateException(
                    "La fecha de registro no puede modificarse."
            );
        }

        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getSeveridad() {
        return severidad;
    }

    public void setSeveridad(String severidad) {
        this.severidad = severidad;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }
}