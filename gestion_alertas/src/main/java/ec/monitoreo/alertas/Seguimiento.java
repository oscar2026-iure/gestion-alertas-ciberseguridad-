package ec.monitoreo.alertas;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

/** Novedades anexadas al expediente. Las correcciones se registran como otra novedad. */
@Entity
@Table(name = "seguimiento", indexes = @Index(name="ix_seguimiento_alerta_fecha", columnList="alerta_id,fecha_actividad"))
public class Seguimiento {
    public static final int MAX_AUTOR=150, MAX_TIPO_ACCION=30,
            MAX_DESCRIPCION=1000, MAX_RESULTADO=500, MAX_OTRO_DETALLE=100;
    private static final Set<String> TIPOS=Set.of("ANALISIS", "CONTENCION", "RECUPERACION", "COMUNICACION", "CIERRE", "CORRECCION", "OTRA");
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="alerta_id", nullable=false, updatable=false)
    private Alerta alerta;
    @Column(name="fecha_registro", nullable=false, updatable=false)
    private LocalDateTime fechaRegistro;
    @Column(name="fecha_actividad", nullable=false, updatable=false)
    private LocalDateTime fechaActividad;
    @Column(nullable=false, length=MAX_AUTOR, updatable=false)
    private String autor;
    @Column(name="tipo_accion", nullable=false, length=MAX_TIPO_ACCION, updatable=false)
    private String tipoAccion;
    @Column(nullable=false, length=MAX_DESCRIPCION, updatable=false)
    private String descripcion;
    @Column(length=MAX_RESULTADO, updatable=false)
    private String resultado;
    @Column(name="otro_detalle", length=MAX_OTRO_DETALLE, updatable=false)
    private String otroDetalle;

    protected Seguimiento() { }
    public Seguimiento(Alerta alerta, LocalDateTime fechaActividad, String autor,
            String tipoAccion, String descripcion, String resultado, String otroDetalle) {
        this.alerta=alerta; this.fechaActividad=fechaActividad;
        this.autor=limpiar(autor); this.tipoAccion=tipoAccion;
        this.descripcion=limpiar(descripcion); this.resultado=limpiar(resultado);
        this.otroDetalle=limpiar(otroDetalle); this.fechaRegistro=LocalDateTime.now();
        validarDatos();
    }
    private static String limpiar(String s) { return s==null?null:s.strip(); }
    private static void texto(String campo, String valor, int limite, boolean requerido) {
        if(requerido && (valor==null || valor.isBlank())) throw new IllegalArgumentException(campo+" es obligatorio.");
        if(valor!=null && valor.length()>limite) throw new IllegalArgumentException(campo+": máximo "+limite+" caracteres.");
    }
    @PrePersist
    private void antesDeRegistrar() {
        if(fechaRegistro==null) fechaRegistro=LocalDateTime.now();
        validarDatos();
    }
    @PreUpdate
    private void impedirEdicion() { throw new IllegalStateException("Registre una novedad de CORRECCION para conservar el historial."); }
    public void validarDatos() {
        if(alerta==null || alerta.getId()==null) throw new IllegalArgumentException("Seleccione una alerta guardada.");
        if(fechaActividad==null || fechaRegistro==null) throw new IllegalArgumentException("Las fechas son obligatorias.");
        if(fechaActividad.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("La actividad no puede tener una fecha futura.");
        texto("Autor",autor,MAX_AUTOR,true); texto("Tipo",tipoAccion,MAX_TIPO_ACCION,true);
        if(!TIPOS.contains(tipoAccion)) throw new IllegalArgumentException("Tipo de acción inválido.");
        texto("Descripción",descripcion,MAX_DESCRIPCION,true);
        texto("Resultado",resultado,MAX_RESULTADO,false); texto("Otro detalle",otroDetalle,MAX_OTRO_DETALLE,false);
    }
    public Long getId(){ return id; }
    public Alerta getAlerta(){ return alerta; }
    public LocalDateTime getFechaRegistro(){ return fechaRegistro; }
    public LocalDateTime getFechaActividad(){ return fechaActividad; }
    public String getAutor(){ return autor; }
    public String getTipoAccion(){ return tipoAccion; }
    public String getDescripcion(){ return descripcion; }
    public String getResultado(){ return resultado; }
    public String getOtroDetalle(){ return otroDetalle; }
}
