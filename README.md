# Gestión de alertas de ciberseguridad completa

Proyecto académico de Persistencia de Datos, Unidad 2.
Proyecto académico de Persistencia de Datos desarrollado en Java con Swing, JPA, Hibernate y PostgreSQL. Permite gestionar alertas de ciberseguridad, registrar hallazgos técnicos y consultar el historial de seguimiento.

Autor: Oscar Legarda

## Tecnologías

- Java y Swing
- Jakarta Persistence
- Hibernate
- PostgreSQL
- Maven
- Apache NetBeans

## Funcionalidades

- Registrar, consultar y actualizar alertas.
- Anular alertas mediante baja lógica.
- Registrar detalles técnicos.
- Consultar el expediente de una alerta.
- Registrar novedades y consultar su historial.

## Configuración

1. Abrir el proyecto Maven en NetBeans.
2. Crear una base PostgreSQL llamada bd_grupociberdefensa.
3. Configurar la conexión local en:
   src/main/resources/META-INF/persistence.xml
4. Configurar la contraseña local de PostgreSQL.
5. Limpiar y construir el proyecto.
6. Ejecutar Gestion_alertas.

## Funciones pendientes

- Formulario para vincular alertas.
- Identificación del creador de cada alerta.
- Auditoría de modificaciones.

Los datos de ejemplo corresponden a una práctica académica.
