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

import java.util.Set;

@Entity
@Table(name = "detalle_tecnico")
public class DetalleTecnico {

    // Límites para los campos de texto.
    public static final int MAX_TIPO_HALLAZGO = 30;
    public static final int MAX_DESCRIPCION = 1000;
    public static final int MAX_CVE = 30;
    public static final int MAX_TTP = 300;
    public static final int MAX_VECTOR_ATAQUE = 300;
    public static final int MAX_ESTADO_VALIDACION = 20;
    public static final int MAX_INSTITUCION = 150;
    public static final int MAX_OTRO_DETALLE = 100;

    private static final Set<String> TIPOS_PERMITIDOS =
            Set.of(
                    "VULNERABILIDAD",
                    "TTP",
                    "VECTOR_ATAQUE",
                    "IOC",
                    "IOA",
                    "ARCHIVO"
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
            name = "tipo_hallazgo",
            nullable = false,
            length = MAX_TIPO_HALLAZGO
    )
    private String tipoHallazgo;

    @Column(
            name = "descripcion",
            nullable = false,
            length = MAX_DESCRIPCION
    )
    private String descripcion;

    @Column(name = "cve", length = MAX_CVE)
    private String cve;

    @Column(name = "ttp", length = MAX_TTP)
    private String ttp;

    @Column(
            name = "vector_ataque",
            length = MAX_VECTOR_ATAQUE
    )
    private String vectorAtaque;

    @Column(
            name = "estado_validacion",
            nullable = false,
            length = MAX_ESTADO_VALIDACION
    )
    private String estadoValidacion = "PENDIENTE";

    @Column(
            name = "institucion_afectada",
            length = MAX_INSTITUCION
    )
    private String institucionAfectada;

    @Column(
            name = "otro_detalle",
            length = MAX_OTRO_DETALLE
    )
    private String otroDetalle;

    // Muchos detalles técnicos pertenecen a una alerta.
    // LAZY evita cargar la alerta automáticamente.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    // Constructor requerido por JPA.
    public DetalleTecnico() {
    }

    public DetalleTecnico(
            Alerta alerta,
            String tipoHallazgo,
            String descripcion) {

        this.alerta = alerta;
        this.tipoHallazgo = tipoHallazgo;
        this.descripcion = descripcion;
        this.estadoValidacion = "PENDIENTE";
    }

    @PrePersist
    @PreUpdate
    private void antesDeGuardar() {
        validarDatos();
    }

    public void validarDatos() {

        validarTexto(
                "Tipo de hallazgo",
                tipoHallazgo,
                MAX_TIPO_HALLAZGO,
                true
        );

        validarTexto(
                "Descripción",
                descripcion,
                MAX_DESCRIPCION,
                true
        );

        validarTexto("CVE", cve, MAX_CVE, false);
        validarTexto("TTP", ttp, MAX_TTP, false);

        validarTexto(
                "Vector de ataque",
                vectorAtaque,
                MAX_VECTOR_ATAQUE,
                false
        );

        validarTexto(
                "Estado de validación",
                estadoValidacion,
                MAX_ESTADO_VALIDACION,
                true
        );

        validarTexto(
                "Institución afectada",
                institucionAfectada,
                MAX_INSTITUCION,
                false
        );

        validarTexto(
                "Otro detalle",
                otroDetalle,
                MAX_OTRO_DETALLE,
                false
        );

        if (alerta == null) {
            throw new IllegalArgumentException(
                    "El detalle técnico debe pertenecer a una alerta."
            );
        }

        if (!TIPOS_PERMITIDOS.contains(tipoHallazgo)) {
            throw new IllegalArgumentException(
                    "Tipo de hallazgo no válido. Utilice: "
                    + "VULNERABILIDAD, TTP, VECTOR_ATAQUE, "
                    + "IOC, IOA o ARCHIVO."
            );
        }

        if (!ESTADOS_PERMITIDOS.contains(estadoValidacion)) {
            throw new IllegalArgumentException(
                    "Estado de validación no válido. Utilice: "
                    + "PENDIENTE, EN_VALIDACION, "
                    + "CONFIRMADO o DESCARTADO."
            );
        }

        // CVE es opcional: no toda vulnerabilidad tiene uno.
        if (cve != null && !cve.isBlank()
                && !cve.matches("CVE-[0-9]{4}-[0-9]{4,}")) {

            throw new IllegalArgumentException(
                    "El CVE debe tener el formato CVE-AAAA-NNNN, "
                    + "con al menos cuatro dígitos finales."
            );
        }

        if ("TTP".equals(tipoHallazgo)
                && (ttp == null || ttp.isBlank())) {

            throw new IllegalArgumentException(
                    "Detalle la TTP observada en el campo TTP."
            );
        }

        if ("VECTOR_ATAQUE".equals(tipoHallazgo)
                && (vectorAtaque == null
                || vectorAtaque.isBlank())) {

            throw new IllegalArgumentException(
                    "Describa el vector o indique que es desconocido."
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

    public String getTipoHallazgo() {
        return tipoHallazgo;
    }

    public void setTipoHallazgo(String tipoHallazgo) {
        this.tipoHallazgo = tipoHallazgo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCve() {
        return cve;
    }

    public void setCve(String cve) {
        this.cve = cve;
    }

    public String getTtp() {
        return ttp;
    }

    public void setTtp(String ttp) {
        this.ttp = ttp;
    }

    public String getVectorAtaque() {
        return vectorAtaque;
    }

    public void setVectorAtaque(String vectorAtaque) {
        this.vectorAtaque = vectorAtaque;
    }

    public String getEstadoValidacion() {
        return estadoValidacion;
    }

    public void setEstadoValidacion(String estadoValidacion) {
        this.estadoValidacion = estadoValidacion;
    }

    public String getInstitucionAfectada() {
        return institucionAfectada;
    }

    public void setInstitucionAfectada(String institucionAfectada) {
        this.institucionAfectada = institucionAfectada;
    }

    public String getOtroDetalle() {
        return otroDetalle;
    }

    public void setOtroDetalle(String otroDetalle) {
        this.otroDetalle = otroDetalle;
    }

    public Alerta getAlerta() {
        return alerta;
    }

    public void setAlerta(Alerta alerta) {
        this.alerta = alerta;
    }
}