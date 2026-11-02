package ec.monitoreo.alertas;

import jakarta.persistence.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;

/** Consulta separada del expediente y registro de avances. */
public class PanelTrazabilidad extends JPanel {
    private final EntityManagerFactory fabrica;
    private final java.util.function.Consumer<Boolean> bloqueoGlobal;
    private boolean ocupado;
    private final java.util.function.BooleanSupplier ocupadoGlobal;
    private Long seleccionada;
    private final List<JButton> botones=new ArrayList<>();
    private final JTextField buscar=new JTextField(25), autor=new JTextField(25), fecha=new JTextField(20);
    private final JCheckBox anuladas=new JCheckBox("Incluir anuladas");
    private final JLabel estado=new JLabel("Busque y seleccione una alerta.");
    private final JTextArea expediente=new JTextArea(), descripcion=new JTextArea(3,30), resultado=new JTextArea(2,30), otro=new JTextArea(2,30);
    private final JComboBox<String> tipo=new JComboBox<>(new String[]{"ANALISIS","CONTENCION","RECUPERACION","COMUNICACION","CIERRE","CORRECCION","OTRA"});
    private final DefaultTableModel modelo=new DefaultTableModel(new String[]{"ID","Fecha","Descripción","Severidad","Responsable","Estado","Activa"},0) {
        @Override public boolean isCellEditable(int r,int c){return false;}
    };
    private final JTable tabla=new JTable(modelo);
    private static final DateTimeFormatter FORMATO=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);
    public PanelTrazabilidad(EntityManagerFactory fabrica, Consumer<Boolean> bloqueoGlobal, java.util.function.BooleanSupplier ocupadoGlobal) {
        super(new BorderLayout(6,6)); this.fabrica=fabrica; this.bloqueoGlobal=bloqueoGlobal; this.ocupadoGlobal=ocupadoGlobal;
        JPanel superior=new JPanel(new FlowLayout(FlowLayout.LEFT));
        superior.add(new JLabel("Buscar ID, descripción o responsable:")); superior.add(buscar);
        boton(superior,"Buscar",this::consultar); superior.add(anuladas);
        buscar.addActionListener(e->consultar());
        add(superior,BorderLayout.NORTH);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); tabla.setAutoCreateRowSorter(true);
        tabla.getSelectionModel().addListSelectionListener(e->{
            if(!e.getValueIsAdjusting() && !ocupado && tabla.getSelectedRow()>=0){
                int r=tabla.convertRowIndexToModel(tabla.getSelectedRow());
                seleccionada=(Long)modelo.getValueAt(r,0); cargar();
            }
        });
        expediente.setEditable(false); expediente.setLineWrap(true); expediente.setWrapStyleWord(true);
        JPanel formulario=new JPanel(new GridLayout(0,2,6,6));
        campo(formulario,"Autor * — máximo 150",autor);
        campo(formulario,"Fecha actividad * — aaaa-MM-dd HH:mm",fecha);
        campo(formulario,"Tipo de acción",tipo);
        campo(formulario,"Descripción * — máximo 1000",new JScrollPane(descripcion));
        campo(formulario,"Resultado — máximo 500",new JScrollPane(resultado));
        campo(formulario,"Otro detalle — máximo 100",new JScrollPane(otro));
        JPanel ingreso=new JPanel(new BorderLayout(6,6)); ingreso.add(formulario,BorderLayout.CENTER);
        JPanel acciones=new JPanel(new FlowLayout(FlowLayout.LEFT));
        boton(acciones,"Registrar novedad",this::registrar);
        boton(acciones,"Limpiar novedad",this::limpiar);
        boton(acciones,"Refrescar expediente",this::cargar);
        ingreso.add(acciones,BorderLayout.SOUTH);
        JTabbedPane vistas=new JTabbedPane(); vistas.addTab("Expediente completo",new JScrollPane(expediente)); vistas.addTab("Agregar novedad",ingreso);
        JSplitPane division=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(tabla),vistas);
        division.setResizeWeight(.30); add(division,BorderLayout.CENTER); add(estado,BorderLayout.SOUTH);
        limitar(buscar,150); limitar(autor,150); limitar(fecha,16); limitar(descripcion,1000); limitar(resultado,500); limitar(otro,100);
        for(JTextArea a:new JTextArea[]{descripcion,resultado,otro}){a.setLineWrap(true);a.setWrapStyleWord(true);}
        limpiar();
    }
    private void campo(JPanel p,String etiqueta,Component c){p.add(new JLabel(etiqueta));p.add(c);}
    private void boton(JPanel p,String texto,Runnable accion){JButton b=new JButton(texto);b.addActionListener(e->accion.run());botones.add(b);p.add(b);}
    private void limpiar(){descripcion.setText("");resultado.setText("");otro.setText("");fecha.setText(LocalDateTime.now().format(FORMATO));tipo.setSelectedIndex(0);}
    private void consultar(){
        String q=buscar.getText().strip(); boolean incluir=anuladas.isSelected();
        tarea(()->{
            try(EntityManager em=fabrica.createEntityManager()){
                Long id;
                try{id=Long.valueOf(q);}catch(NumberFormatException e){id=-1L;}
                String patron="%"+q.toLowerCase(java.util.Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
                return em.createQuery("select a.id,a.fecha,a.descripcion,a.severidad,a.responsable,a.estado,a.activo from Alerta a where (:todas=true or a.activo=true) and (a.id=:id or lower(a.descripcion) like :q escape '!' or lower(a.responsable) like :q escape '!') order by a.fecha desc,a.id desc",Object[].class)
                    .setParameter("todas",incluir).setParameter("id",id).setParameter("q",patron).setMaxResults(200).getResultList();
            }
        },filas->{
            seleccionada=null; expediente.setText("");modelo.setRowCount(0);
            for(Object[] f:filas)modelo.addRow(f);
            estado.setText("Resultados: "+filas.size()+" (máximo 200). Seleccione una fila para abrir el expediente.");
        });
    }
    private static void seccion(StringBuilder s,String titulo,List<Object[]> filas){
        s.append("\n\n").append(titulo).append("\n");
        if(filas.isEmpty())s.append("Sin registros.\n");
        for(Object[] fila:filas){for(Object v:fila)s.append(v==null?"—":v).append(" | ");s.append("\n\n");}
        if(filas.size()==500)s.append("Se muestran hasta 500 registros en esta sección.\n");
    }
    private void cargar(){
        if(seleccionada==null){estado.setText("Seleccione una alerta en los resultados.");return;}
        final Long id=seleccionada;
        tarea(()->{
            try(EntityManager em=fabrica.createEntityManager()){
                Alerta a=em.find(Alerta.class,id);
                if(a==null)throw new IllegalStateException("La alerta ya no existe.");
                StringBuilder s=new StringBuilder("ALERTA #"+id+"\nFecha: "+a.getFecha()+"\nDescripción: "+a.getDescripcion()+"\nSeveridad: "+a.getSeveridad()+"\nResponsable: "+a.getResponsable()+"\nEstado: "+a.getEstado()+"\nActiva: "+a.isActivo());
                if(!a.isActivo())s.append("\nMotivo de anulación: ").append(a.getMotivoAnulacion());
                seccion(s,"DETALLES TÉCNICOS (ID | tipo | descripción | CVE | TTP | vector | validación | institución | otro)",em.createQuery("select d.id,d.tipoHallazgo,d.descripcion,d.cve,d.ttp,d.vectorAtaque,d.estadoValidacion,d.institucionAfectada,d.otroDetalle from DetalleTecnico d where d.alerta.id=:id order by d.id",Object[].class).setParameter("id",id).setMaxResults(500).getResultList());
                seccion(s,"ALERTAS RELACIONADAS (origen | destino | tipo | motivo | validación | fecha | otro)",em.createQuery("select r.alertaOrigen.id,r.alertaRelacionada.id,r.tipoRelacion,r.motivo,r.estadoValidacion,r.fechaRegistro,r.otroDetalle from RelacionAlerta r where r.alertaOrigen.id=:id or r.alertaRelacionada.id=:id order by r.fechaRegistro,r.id",Object[].class).setParameter("id",id).setMaxResults(500).getResultList());
                seccion(s,"HISTORIAL (ID | fecha actividad | fecha registro | autor declarado | acción | descripción | resultado | otro)",em.createQuery("select s.id,s.fechaActividad,s.fechaRegistro,s.autor,s.tipoAccion,s.descripcion,s.resultado,s.otroDetalle from Seguimiento s where s.alerta.id=:id order by s.fechaActividad,s.fechaRegistro,s.id",Object[].class).setParameter("id",id).setMaxResults(500).getResultList());
                return s.toString();
            }
        },texto->{expediente.setText(texto);expediente.setCaretPosition(0);estado.setText("Expediente de alerta #"+id+". Puede agregar una novedad en la pestaña interna.");});
    }
    private void registrar(){
        if(seleccionada==null){estado.setText("Primero busque y seleccione una alerta.");return;}
        final Long id=seleccionada; final String au=autor.getText(),ac=(String)tipo.getSelectedItem(),de=descripcion.getText(),re=resultado.getText(),ot=otro.getText();
        final LocalDateTime actividad;
        try{actividad=LocalDateTime.parse(fecha.getText(),FORMATO);}
        catch(java.time.format.DateTimeParseException e){JOptionPane.showMessageDialog(this,"Fecha inválida. Ejemplo: 2026-11-01 09:30");return;}
        tarea(()->{
            try(EntityManager em=fabrica.createEntityManager()){
                EntityTransaction tx=em.getTransaction();
                try{
                    tx.begin();
                    Alerta a=em.find(Alerta.class,id,LockModeType.PESSIMISTIC_WRITE);
                    if(a==null || !a.isActivo())throw new IllegalStateException("La alerta no existe o está anulada; solo permite consulta.");
                    Seguimiento s=new Seguimiento(a,actividad,au,ac,de,re,ot);
                    em.persist(s);tx.commit();return s.getId();
                }catch(RuntimeException e){if(tx.isActive())tx.rollback();throw e;}
            }
        },nuevo->{limpiar();cargar();});
    }
    private <T> void tarea(java.util.concurrent.Callable<T> accion,Consumer<T> terminado){
        if(ocupado || ocupadoGlobal.getAsBoolean())return;
        ocupado=true; bloqueoGlobal.accept(true);tabla.setEnabled(false);botones.forEach(b->b.setEnabled(false));estado.setText("Procesando…");
        new SwingWorker<T,Void>(){
            protected T doInBackground()throws Exception{return accion.call();}
            protected void done(){
                ocupado=false;bloqueoGlobal.accept(false);tabla.setEnabled(true);botones.forEach(b->b.setEnabled(true));
                try{terminado.accept(get());}
                catch(InterruptedException e){Thread.currentThread().interrupt();error(e);}
                catch(ExecutionException e){error(e.getCause());}
                catch(RuntimeException e){error(e);}
            }
        }.execute();
    }
    private void error(Throwable e){e.printStackTrace();estado.setText("No se completó la operación.");JOptionPane.showMessageDialog(this,e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
    private static void limitar(JTextComponent c,int max){
        ((AbstractDocument)c.getDocument()).setDocumentFilter(new DocumentFilter(){
            @Override public void insertString(FilterBypass b,int o,String s,AttributeSet a)throws BadLocationException{replace(b,o,0,s,a);}
            @Override public void replace(FilterBypass b,int o,int n,String s,AttributeSet a)throws BadLocationException{
                String t=s==null?"":s;if(b.getDocument().getLength()-n+t.length()<=max)b.replace(o,n,t,a);else Toolkit.getDefaultToolkit().beep();
            }
        });
    }
}
