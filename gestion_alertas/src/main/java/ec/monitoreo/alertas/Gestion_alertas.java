package ec.monitoreo.alertas;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

public class Gestion_alertas extends JFrame {

    private static final int MAX_RESULTADOS = 200;

    private final EntityManagerFactory fabrica;
    private final List<JButton> botones = new ArrayList<>();

    private boolean ocupado = false;

    private Long alertaSeleccionada;
    private Long detalleSeleccionado;

    // =====================================================
    // CAMPOS DE ALERTA
    // =====================================================

    private final JTextField txtId = new JTextField();

    private final JTextArea txtDescripcion =
            new JTextArea(3, 35);

    private final JComboBox<String> cmbSeveridad =
            new JComboBox<>(new String[]{
                "BAJA", "MEDIA", "ALTA", "CRITICA"
            });

    private final JComboBox<String> cmbResponsable =
            new JComboBox<>(new String[]{
                "Oscar Leg",
                "Hernán Val",
                "Luis Lima",
                "Técnico Vacante"
            });

    private final JComboBox<String> cmbEstado =
            new JComboBox<>(new String[]{
                "REGISTRADA",
                "EN_ANALISIS",
                "EN_TRATAMIENTO",
                "CERRADA"
            });

    private final JTextField txtBuscar = new JTextField(25);

    private final JCheckBox chkAnuladas =
            new JCheckBox("Mostrar anuladas");

    private final DefaultTableModel modeloAlertas =
            modeloNoEditable(
                    "ID",
                    "Fecha",
                    "Descripción",
                    "Severidad",
                    "Responsable",
                    "Estado",
                    "Activa",
                    "Motivo de anulación"
            );

    private final JTable tablaAlertas =
            new JTable(modeloAlertas);

    private List<Alerta> alertas = new ArrayList<>();

    // =====================================================
    // CAMPOS DE DETALLE TÉCNICO
    // =====================================================

    private final JLabel lblAlertaDetalle =
            new JLabel("Seleccione una alerta en la primera pestaña.");

    private final JTextField txtDetalleId = new JTextField();

    private final JComboBox<String> cmbTipoHallazgo =
            new JComboBox<>(new String[]{
                "VULNERABILIDAD",
                "ARCHIVO MALICIOSO"
            });

    private final JTextArea txtDetalleDescripcion =
            new JTextArea(3, 35);

    private final JTextField txtCve = new JTextField();
    private final JTextField txtTtp = new JTextField();
    private final JTextField txtVector = new JTextField();
    private final JTextField txtInstitucion = new JTextField();

    private final JComboBox<String> cmbValidacion =
            new JComboBox<>(new String[]{
                "PENDIENTE",
                "EN_VALIDACION",
                "CONFIRMADO",
                "DESCARTADO"
            });

    private final JTextArea txtOtroDetalle =
            new JTextArea(3, 35);

    private final DefaultTableModel modeloDetalles =
            modeloNoEditable(
                    "ID",
                    "Tipo",
                    "Descripción",
                    "CVE",
                    "TTP",
                    "Vector de Ataque",
                    "Validación",
                    "Institución",
                    "Otro detalle"
            );

    private final JTable tablaDetalles =
            new JTable(modeloDetalles);

    /*
     * Solo se guardan en memoria los datos de las columnas.
     * No necesitamos acceder a alerta desde un detalle
     * separado de su EntityManager.
     */
    private List<Object[]> detalles = new ArrayList<>();

    private final JLabel lblInformacion =
            new JLabel("Listo.");

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public Gestion_alertas(EntityManagerFactory fabrica) {

        super("Gestión de alertas de ciberseguridad");

        this.fabrica = fabrica;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1250, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        configurarCampos();

        JTabbedPane pestanas = new JTabbedPane();

        pestanas.addTab("Alertas", construirPanelAlertas());
        pestanas.addTab(
                "Detalle técnico",
                construirPanelDetalles()
        );

        pestanas.addTab("Seguimiento y trazabilidad",
                new PanelTrazabilidad(fabrica, this::cambiarOcupado, () -> ocupado));

        add(pestanas, BorderLayout.CENTER);
        add(lblInformacion, BorderLayout.SOUTH);

        tablaAlertas.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tablaDetalles.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tablaAlertas.setRowHeight(25);
        tablaDetalles.setRowHeight(25);

        tablaAlertas.getSelectionModel()
                .addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting() && !ocupado) {
                        cargarAlertaSeleccionada();
                    }
                });

        tablaDetalles.getSelectionModel()
                .addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting() && !ocupado) {
                        cargarDetalleSeleccionado();
                    }
                });

        chkAnuladas.addActionListener(e -> consultar());
        txtBuscar.addActionListener(e -> consultar());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarAplicacion();
            }
        });

        limpiar();
        consultar();
    }

    // =====================================================
    // CONFIGURACIÓN DE CAMPOS
    // =====================================================

    private void configurarCampos() {

        txtId.setEditable(false);
        txtDetalleId.setEditable(false);

        configurarArea(txtDescripcion);
        configurarArea(txtDetalleDescripcion);
        configurarArea(txtOtroDetalle);

        limitar(
                txtDescripcion,
                Alerta.MAX_DESCRIPCION
        );

        limitar(txtBuscar, 150);

        limitar(
                txtDetalleDescripcion,
                DetalleTecnico.MAX_DESCRIPCION
        );

        limitar(txtCve, DetalleTecnico.MAX_CVE);
        limitar(txtTtp, DetalleTecnico.MAX_TTP);

        limitar(
                txtVector,
                DetalleTecnico.MAX_VECTOR_ATAQUE
        );

        limitar(
                txtInstitucion,
                DetalleTecnico.MAX_INSTITUCION
        );

        limitar(
                txtOtroDetalle,
                DetalleTecnico.MAX_OTRO_DETALLE
        );

        txtDescripcion.setToolTipText(
                "Obligatorio. Máximo 1000 caracteres."
        );

        txtDetalleDescripcion.setToolTipText(
                "Describa el hallazgo y su contexto. "
                + "Máximo 1000 caracteres."
        );

        txtOtroDetalle.setToolTipText(
                "Observación adicional. Máximo 100 caracteres."
        );
    }

    private void configurarArea(JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
    }

    // =====================================================
    // CONSTRUCCIÓN DE LA PANTALLA
    // =====================================================

    private JPanel construirPanelAlertas() {

        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel formulario =
                new JPanel(new GridLayout(0, 2, 8, 8));

        formulario.setBorder(
                BorderFactory.createTitledBorder(
                        "Datos de la alerta"
                )
        );

        agregarCampo(formulario, "ID:", txtId);

        agregarCampo(
                formulario,
                "Descripción * — máximo 1000:",
                new JScrollPane(txtDescripcion)
        );

        agregarCampo(
                formulario,
                "Severidad:",
                cmbSeveridad
        );

        agregarCampo(
                formulario,
                "Responsable:",
                cmbResponsable
        );

        agregarCampo(formulario, "Estado:", cmbEstado);

        JPanel acciones =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        agregarBoton(acciones, "Nuevo / Limpiar", this::limpiar);
        agregarBoton(acciones, "Registrar", this::registrar);
        agregarBoton(acciones, "Actualizar", this::actualizar);
        agregarBoton(acciones, "Anular", this::anular);

        JPanel busqueda =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        busqueda.add(
                new JLabel("Buscar descripción o responsable:")
        );

        busqueda.add(txtBuscar);

        agregarBoton(busqueda, "Buscar / Refrescar", this::consultar);

        agregarBoton(busqueda, "Limpiar búsqueda", () -> {
            txtBuscar.setText("");
            consultar();
        });

        busqueda.add(chkAnuladas);

        JPanel superior = new JPanel(new BorderLayout(5, 5));
        superior.add(formulario, BorderLayout.CENTER);

        JPanel inferiorFormulario =
                new JPanel(new BorderLayout());

        inferiorFormulario.add(acciones, BorderLayout.NORTH);
        inferiorFormulario.add(busqueda, BorderLayout.SOUTH);

        superior.add(inferiorFormulario, BorderLayout.SOUTH);

        panel.add(superior, BorderLayout.NORTH);
        panel.add(
                new JScrollPane(tablaAlertas),
                BorderLayout.CENTER
        );

        return panel;
    }

    private JPanel construirPanelDetalles() {

        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel formulario =
                new JPanel(new GridLayout(0, 2, 8, 6));

        formulario.setBorder(
                BorderFactory.createTitledBorder(
                        "Hallazgo técnico"
                )
        );

        agregarCampo(formulario, "ID del detalle:", txtDetalleId);
        agregarCampo(formulario, "Tipo de hallazgo *:", cmbTipoHallazgo);

        agregarCampo(
                formulario,
                "Descripción * — máximo 1000:",
                new JScrollPane(txtDetalleDescripcion)
        );

        agregarCampo(formulario, "CVE — máximo 30:", txtCve);
        agregarCampo(formulario, "TTP — máximo 300:", txtTtp);

        agregarCampo(
                formulario,
                "Vector de ataque — máximo 300:",
                txtVector
        );

        agregarCampo(
                formulario,
                "Estado de validación:",
                cmbValidacion
        );

        agregarCampo(
                formulario,
                "Institución afectada — máximo 150:",
                txtInstitucion
        );

        agregarCampo(
                formulario,
                "Otro detalle — máximo 100:",
                new JScrollPane(txtOtroDetalle)
        );

        JPanel acciones =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        agregarBoton(
                acciones,
                "Nuevo detalle",
                this::limpiarDetalle
        );

        agregarBoton(
                acciones,
                "Registrar detalle",
                () -> guardarDetalle(false)
        );

        agregarBoton(
                acciones,
                "Actualizar detalle",
                () -> guardarDetalle(true)
        );

        agregarBoton(
                acciones,
                "Consultar detalles",
                this::consultarDetalles
        );

        JPanel superior = new JPanel(new BorderLayout(5, 5));

        superior.add(lblAlertaDetalle, BorderLayout.NORTH);
        superior.add(formulario, BorderLayout.CENTER);
        superior.add(acciones, BorderLayout.SOUTH);

        panel.add(superior, BorderLayout.NORTH);

        panel.add(
                new JScrollPane(tablaDetalles),
                BorderLayout.CENTER
        );

        return panel;
    }

    private void agregarCampo(
            JPanel panel,
            String etiqueta,
            JComponent componente) {

        panel.add(new JLabel(etiqueta));
        panel.add(componente);
    }

    private void agregarBoton(
            JPanel panel,
            String texto,
            Runnable accion) {

        JButton boton = new JButton(texto);

        boton.addActionListener(e -> {
            if (!ocupado) {
                accion.run();
            }
        });

        botones.add(boton);
        panel.add(boton);
    }

    private static DefaultTableModel modeloNoEditable(
            String... columnas) {

        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    // =====================================================
    // ALERTAS
    // =====================================================

    private void limpiar() {

        alertaSeleccionada = null;

        tablaAlertas.clearSelection();

        txtId.setText("");
        txtDescripcion.setText("");

        cmbSeveridad.setSelectedItem("MEDIA");
        cmbResponsable.setSelectedItem("Técnico Vacante");
        cmbEstado.setSelectedItem("REGISTRADA");

        limpiarDetalle();
        modeloDetalles.setRowCount(0);
        detalles = new ArrayList<>();

        lblAlertaDetalle.setText(
                "Seleccione una alerta en la primera pestaña."
        );

        lblInformacion.setText(
                "Formulario listo para registrar una alerta."
        );
    }

    private Alerta leerFormularioAlerta() {

        Alerta datos = new Alerta(
                txtDescripcion.getText().trim(),
                seleccionado(cmbSeveridad),
                seleccionado(cmbResponsable)
        );

        datos.setEstado(seleccionado(cmbEstado));
        datos.validarDatos();

        return datos;
    }

    private void registrar() {

        if (alertaSeleccionada != null) {
            mensaje("Pulse Nuevo / Limpiar para registrar otra alerta.");
            return;
        }

        final Alerta nueva;

        try {
            nueva = leerFormularioAlerta();
            nueva.setEstado("REGISTRADA");
        } catch (RuntimeException error) {
            mostrarError(error);
            return;
        }

        ejecutar(
                "Registrando alerta...",
                () -> transaccion(gestor -> {
                    gestor.persist(nueva);
                    return nueva.getId();
                }),
                id -> {
                    mensaje("Alerta registrada con ID " + id + ".");
                    consultar();
                }
        );
    }

    private void consultar() {

        if (ocupado) {
            return;
        }

        final String busqueda =
                txtBuscar.getText().trim().toLowerCase(
                        java.util.Locale.ROOT
                );

        final boolean incluirAnuladas =
                chkAnuladas.isSelected();

        ejecutar(
                "Consultando alertas...",
                () -> {
                    try (EntityManager gestor =
                            fabrica.createEntityManager()) {

                        String jpql =
                                "SELECT a FROM Alerta a WHERE 1 = 1";

                        if (!incluirAnuladas) {
                            jpql += " AND a.activo = true";
                        }

                        if (!busqueda.isBlank()) {
                            jpql +=
                                    " AND (LOWER(a.descripcion) "
                                    + "LIKE :texto ESCAPE '!'"
                                    + " OR LOWER(a.responsable) "
                                    + "LIKE :texto ESCAPE '!')";
                        }

                        jpql += " ORDER BY a.id DESC";

                        TypedQuery<Alerta> consulta =
                                gestor.createQuery(jpql, Alerta.class);

                        if (!busqueda.isBlank()) {
                            consulta.setParameter(
                                    "texto",
                                    "%" + escaparLike(busqueda) + "%"
                            );
                        }

                        return consulta
                                .setMaxResults(MAX_RESULTADOS)
                                .getResultList();
                    }
                },
                resultado -> {

                    limpiar();

                    modeloAlertas.setRowCount(0);
                    alertas = resultado;

                    for (Alerta a : alertas) {
                        modeloAlertas.addRow(new Object[]{
                            a.getId(),
                            a.getFecha(),
                            a.getDescripcion(),
                            a.getSeveridad(),
                            a.getResponsable(),
                            a.getEstado(),
                            a.isActivo() ? "SÍ" : "NO",
                            a.getMotivoAnulacion()
                        });
                    }

                    lblInformacion.setText(
                            "Resultados: " + alertas.size()
                            + " | Máximo mostrado: "
                            + MAX_RESULTADOS
                    );
                }
        );
    }

    private static String escaparLike(String texto) {
        return texto
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private void cargarAlertaSeleccionada() {

        int fila = tablaAlertas.getSelectedRow();

        if (fila < 0) {
            return;
        }

        int indice = tablaAlertas.convertRowIndexToModel(fila);

        if (indice >= alertas.size()) {
            return;
        }

        Alerta a = alertas.get(indice);

        alertaSeleccionada = a.getId();

        txtId.setText(a.getId().toString());
        txtDescripcion.setText(a.getDescripcion());

        cmbSeveridad.setSelectedItem(a.getSeveridad());
        cmbEstado.setSelectedItem(a.getEstado());

        /*
         * Permite mostrar un responsable histórico
         * que ya no figure en el listado.
         */
        String responsable = a.getResponsable();

        if (!contiene(cmbResponsable, responsable)) {
            cmbResponsable.addItem(responsable);
        }

        cmbResponsable.setSelectedItem(responsable);

        limpiarDetalle();
        modeloDetalles.setRowCount(0);
        detalles = new ArrayList<>();

        lblAlertaDetalle.setText(
                "Alerta seleccionada: " + a.getId()
                + (a.isActivo() ? "" : " — ANULADA: solo consulta")
        );

        lblInformacion.setText(
                "Alerta " + a.getId()
                + " seleccionada. Puede consultar sus detalles."
        );
    }

    private void actualizar() {

        if (!exigirAlerta()) {
            return;
        }

        final Long id = alertaSeleccionada;
        final Alerta datos;

        try {
            datos = leerFormularioAlerta();
        } catch (RuntimeException error) {
            mostrarError(error);
            return;
        }

        ejecutar(
                "Actualizando alerta...",
                () -> transaccion(gestor -> {

                    Alerta a = buscarActiva(gestor, id);

                    a.setDescripcion(datos.getDescripcion());
                    a.setSeveridad(datos.getSeveridad());
                    a.setResponsable(datos.getResponsable());
                    a.setEstado(datos.getEstado());

                    a.validarDatos();

                    return a.getId();
                }),
                actualizado -> {
                    mensaje("Alerta actualizada.");
                    consultar();
                }
        );
    }

    private void anular() {

        if (!exigirAlerta()) {
            return;
        }

        String motivo = JOptionPane.showInputDialog(
                this,
                "Motivo de anulación. Máximo 500 caracteres:"
        );

        if (motivo == null) {
            return;
        }

        motivo = motivo.trim();

        if (motivo.isBlank()
                || motivo.length() > Alerta.MAX_MOTIVO_ANULACION) {

            mensaje("El motivo debe tener entre 1 y 500 caracteres.");
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Anular la alerta " + alertaSeleccionada
                + "?\nEl registro y sus detalles se conservarán.",
                "Confirmar anulación",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        final Long id = alertaSeleccionada;
        final String motivoFinal = motivo;

        ejecutar(
                "Anulando alerta...",
                () -> transaccion(gestor -> {

                    Alerta a = buscarActiva(gestor, id);

                    a.setActivo(false);
                    a.setMotivoAnulacion(motivoFinal);

                    a.validarDatos();

                    return a.getId();
                }),
                anulado -> {
                    mensaje("Alerta anulada. El registro se conserva.");
                    consultar();
                }
        );
    }

    // =====================================================
    // DETALLES TÉCNICOS
    // =====================================================

    private void limpiarDetalle() {

        detalleSeleccionado = null;

        tablaDetalles.clearSelection();
        txtDetalleId.setText("");

        cmbTipoHallazgo.setSelectedItem("VULNERABILIDAD");
        cmbValidacion.setSelectedItem("PENDIENTE");

        txtDetalleDescripcion.setText("");
        txtCve.setText("");
        txtTtp.setText("");
        txtVector.setText("");
        txtInstitucion.setText("");
        txtOtroDetalle.setText("");
    }

    private DetalleTecnico leerFormularioDetalle() {

        DetalleTecnico datos = new DetalleTecnico();

        datos.setTipoHallazgo(seleccionado(cmbTipoHallazgo));

        datos.setDescripcion(
                txtDetalleDescripcion.getText().trim()
        );

        datos.setCve(
                txtCve.getText().trim().toUpperCase(
                        java.util.Locale.ROOT
                )
        );

        datos.setTtp(txtTtp.getText().trim());
        datos.setVectorAtaque(txtVector.getText().trim());

        datos.setEstadoValidacion(
                seleccionado(cmbValidacion)
        );

        datos.setInstitucionAfectada(
                txtInstitucion.getText().trim()
        );

        datos.setOtroDetalle(
                txtOtroDetalle.getText().trim()
        );

        return datos;
    }

    private void guardarDetalle(boolean actualizar) {

        if (!exigirAlerta()) {
            return;
        }

        if (actualizar && detalleSeleccionado == null) {
            mensaje("Seleccione un detalle de la tabla.");
            return;
        }

        if (!actualizar && detalleSeleccionado != null) {
            mensaje("Pulse Nuevo detalle antes de registrar otro.");
            return;
        }

        final Long alertaId = alertaSeleccionada;
        final Long detalleId = detalleSeleccionado;

        final DetalleTecnico datos = leerFormularioDetalle();

        ejecutar(
                actualizar
                        ? "Actualizando detalle..."
                        : "Registrando detalle...",
                () -> transaccion(gestor -> {

                    Alerta alerta =
                            buscarActiva(gestor, alertaId);

                    DetalleTecnico destino;

                    if (actualizar) {

                        destino = gestor.find(
                                DetalleTecnico.class,
                                detalleId
                        );

                        if (destino == null) {
                            throw new IllegalStateException(
                                    "El detalle ya no existe."
                            );
                        }

                        if (!alertaId.equals(
                                destino.getAlerta().getId())) {

                            throw new IllegalStateException(
                                    "El detalle no pertenece "
                                    + "a la alerta seleccionada."
                            );
                        }

                    } else {
                        destino = new DetalleTecnico();
                        destino.setAlerta(alerta);
                    }

                    copiarDetalle(datos, destino);
                    destino.validarDatos();

                    if (!actualizar) {
                        gestor.persist(destino);
                    }

                    return destino.getId();
                }),
                id -> {
                    mensaje("Detalle guardado correctamente.");
                    consultarDetalles();
                }
        );
    }

    private void copiarDetalle(
            DetalleTecnico origen,
            DetalleTecnico destino) {

        destino.setTipoHallazgo(origen.getTipoHallazgo());
        destino.setDescripcion(origen.getDescripcion());
        destino.setCve(origen.getCve());
        destino.setTtp(origen.getTtp());
        destino.setVectorAtaque(origen.getVectorAtaque());
        destino.setEstadoValidacion(origen.getEstadoValidacion());

        destino.setInstitucionAfectada(
                origen.getInstitucionAfectada()
        );

        destino.setOtroDetalle(origen.getOtroDetalle());
    }

    private void consultarDetalles() {

        if (!exigirAlerta() || ocupado) {
            return;
        }

        final Long id = alertaSeleccionada;

        ejecutar(
                "Consultando detalles...",
                () -> {
                    try (EntityManager gestor =
                            fabrica.createEntityManager()) {

                        /*
                         * Proyección: recupera únicamente las
                         * columnas necesarias para esta pantalla.
                         */
                        return gestor.createQuery(
                                "SELECT d.id, d.tipoHallazgo, "
                                + "d.descripcion, d.cve, d.ttp, "
                                + "d.vectorAtaque, d.estadoValidacion, "
                                + "d.institucionAfectada, d.otroDetalle "
                                + "FROM DetalleTecnico d "
                                + "WHERE d.alerta.id = :id "
                                + "ORDER BY d.id DESC",
                                Object[].class
                        )
                                .setParameter("id", id)
                                .setMaxResults(MAX_RESULTADOS)
                                .getResultList();
                    }
                },
                resultado -> {

                    limpiarDetalle();

                    modeloDetalles.setRowCount(0);
                    detalles = resultado;

                    for (Object[] fila : detalles) {
                        modeloDetalles.addRow(fila);
                    }

                    lblInformacion.setText(
                            "Alerta " + id
                            + " | Detalles mostrados: "
                            + detalles.size()
                            + " | Máximo: " + MAX_RESULTADOS
                    );
                }
        );
    }

    private void cargarDetalleSeleccionado() {

        int fila = tablaDetalles.getSelectedRow();

        if (fila < 0) {
            return;
        }

        int indice = tablaDetalles.convertRowIndexToModel(fila);

        if (indice >= detalles.size()) {
            return;
        }

        Object[] d = detalles.get(indice);

        detalleSeleccionado = (Long) d[0];

        txtDetalleId.setText(texto(d[0]));
        cmbTipoHallazgo.setSelectedItem(texto(d[1]));

        txtDetalleDescripcion.setText(texto(d[2]));
        txtCve.setText(texto(d[3]));
        txtTtp.setText(texto(d[4]));
        txtVector.setText(texto(d[5]));

        cmbValidacion.setSelectedItem(texto(d[6]));

        txtInstitucion.setText(texto(d[7]));
        txtOtroDetalle.setText(texto(d[8]));
    }

    // =====================================================
    // BASE DE DATOS Y SEGUNDO PLANO
    // =====================================================

    @FunctionalInterface
    private interface Tarea<T> {
        T realizar() throws Exception;
    }

    private <T> void ejecutar(
            String informacion,
            Tarea<T> tarea,
            Consumer<T> alTerminar) {

        if (ocupado) {
            return;
        }

        cambiarOcupado(true);
        lblInformacion.setText(informacion);

        new SwingWorker<T, Void>() {

            @Override
            protected T doInBackground() throws Exception {
                return tarea.realizar();
            }

            @Override
            protected void done() {

                cambiarOcupado(false);

                try {
                    T resultado = get();
                    lblInformacion.setText("Operación completada.");
                    alTerminar.accept(resultado);

                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    mostrarError(error);

                } catch (ExecutionException error) {
                    mostrarError(error.getCause());

                } catch (RuntimeException error) {
                    mostrarError(error);
                }
            }

        }.execute();
    }

    private <T> T transaccion(
            Function<EntityManager, T> accion) {

        try (EntityManager gestor =
                fabrica.createEntityManager()) {

            EntityTransaction tx = gestor.getTransaction();

            try {
                tx.begin();

                T resultado = accion.apply(gestor);

                tx.commit();
                return resultado;

            } catch (RuntimeException error) {

                if (tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (RuntimeException falloRollback) {
                        error.addSuppressed(falloRollback);
                    }
                }

                throw error;
            }
        }
    }

    private Alerta buscarActiva(
            EntityManager gestor,
            Long id) {

        Alerta alerta = gestor.find(Alerta.class, id);

        if (alerta == null) {
            throw new IllegalStateException(
                    "La alerta no existe."
            );
        }

        if (!alerta.isActivo()) {
            throw new IllegalStateException(
                    "La alerta está anulada. Solo permite consulta."
            );
        }

        return alerta;
    }

    private void cambiarOcupado(boolean valor) {

        ocupado = valor;

        for (JButton boton : botones) {
            boton.setEnabled(!valor);
        }

        tablaAlertas.setEnabled(!valor);
        tablaDetalles.setEnabled(!valor);
        chkAnuladas.setEnabled(!valor);
    }

    // =====================================================
    // UTILIDADES
    // =====================================================

    private boolean exigirAlerta() {

        if (alertaSeleccionada == null) {
            mensaje("Seleccione primero una alerta.");
            return false;
        }

        return true;
    }

    private static String seleccionado(
            JComboBox<String> combo) {

        Object valor = combo.getSelectedItem();
        return valor == null ? "" : valor.toString();
    }

    private static boolean contiene(
            JComboBox<String> combo,
            String valor) {

        for (int i = 0; i < combo.getItemCount(); i++) {
            if (java.util.Objects.equals(
                    combo.getItemAt(i), valor)) {
                return true;
            }
        }

        return false;
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private void mensaje(String texto) {
        JOptionPane.showMessageDialog(this, texto);
    }

    private void mostrarError(Throwable error) {

        if (error == null) {
            mensaje("No se pudo completar la operación.");
            return;
        }

        error.printStackTrace();

        Throwable causa = error;

        while (causa.getCause() != null
                && causa.getCause() != causa) {
            causa = causa.getCause();
        }

        String detalle = causa.getMessage();

        if (detalle == null || detalle.isBlank()) {
            detalle = causa.getClass().getSimpleName();
        }

        lblInformacion.setText("Operación no completada.");

        JOptionPane.showMessageDialog(
                this,
                "No se pudo completar la operación.\n" + detalle,
                "Revisar operación",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static void limitar(
            JTextComponent campo,
            int maximo) {

        AbstractDocument documento =
                (AbstractDocument) campo.getDocument();

        documento.setDocumentFilter(new DocumentFilter() {

            @Override
            public void insertString(
                    FilterBypass fb,
                    int offset,
                    String texto,
                    AttributeSet atributos)
                    throws BadLocationException {

                replace(fb, offset, 0, texto, atributos);
            }

            @Override
            public void replace(
                    FilterBypass fb,
                    int offset,
                    int longitud,
                    String texto,
                    AttributeSet atributos)
                    throws BadLocationException {

                String entrada = texto == null ? "" : texto;

                int nuevaLongitud =
                        fb.getDocument().getLength()
                        - longitud
                        + entrada.length();

                if (nuevaLongitud <= maximo) {
                    fb.replace(
                            offset,
                            longitud,
                            entrada,
                            atributos
                    );
                } else {
                    java.awt.Toolkit
                            .getDefaultToolkit()
                            .beep();
                }
            }
        });
    }

    private void cerrarAplicacion() {

        if (ocupado) {
            mensaje(
                    "Espere a que termine la operación "
                    + "antes de cerrar."
            );
            return;
        }

        try {
            if (fabrica.isOpen()) {
                fabrica.close();
            }

            dispose();

        } catch (RuntimeException error) {
            mostrarError(error);
        }
    }

    // =====================================================
    // INICIO
    // =====================================================

    public static void main(String[] args) {

        try {
            // Mantener el arranque en el hilo principal hasta terminar
            // la conexión y la preparación del esquema.
            EntityManagerFactory fabrica =
                    Persistence.createEntityManagerFactory("AlertasPU");

            SwingUtilities.invokeLater(() -> {
                try {
                    Gestion_alertas ventana =
                            new Gestion_alertas(fabrica);
                    ventana.setVisible(true);

                } catch (RuntimeException error) {
                    error.printStackTrace();

                    try {
                        if (fabrica.isOpen()) {
                            fabrica.close();
                        }
                    } catch (RuntimeException cierre) {
                        error.addSuppressed(cierre);
                        cierre.printStackTrace();
                    }

                    JOptionPane.showMessageDialog(
                            null,
                            "No se pudo abrir la pantalla.\n"
                            + "Revise Output de NetBeans.",
                            "Error de interfaz",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            });

        } catch (RuntimeException error) {
            error.printStackTrace();

            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(
                            null,
                            "No se pudo iniciar la persistencia.\n"
                            + "Revise la conexión, las entidades y "
                            + "los mensajes de Output de NetBeans.",
                            "Error de persistencia",
                            JOptionPane.ERROR_MESSAGE
                    )
            );
        }
    }
}
