# HotelUQ — Entrega de Programación II

Aplicación de administración hotelera en Java y JavaFX. Las vistas, los servicios y los repositorios se conectan desde `HotelUqApplication`.

## Requisitos para ejecutarla

- JDK 25
- Maven 3.8.5 o superior
- JavaFX 27.0.1 (Maven descarga las dependencias declaradas en `pom.xml`)


```bash
mvn clean test
mvn javafx:run
```

## Inicio limpio (sin información)

La aplicación arranca **sin clientes, tarjetas, habitaciones, reservas, servicios, productos, paquetes, promociones ni beneficios precargados**. Cada registro puede crearse desde la aplicación cuando corresponde. La primera tarjeta del cliente utiliza el mismo identificador que su cuenta (`C-01`, `C-02`...). No se crean tarjetas duplicadas para el mismo cliente.

La pestaña **Administración** tiene como credenciales: `admin` / `hoteluq123`. Son datos para poder usar en la explicación. La sesión administrativa se cierra cuando se abandona la pestaña principal de Administración.

Los datos de clientes, tarjetas, reservas y demás registros usan repositorios **en memoria** y se pierden al reiniciar. Los cierres fiscales son archivos TXT almacenados en `~/.hoteluq/fiscales/`.

## Precarga opcional para la exposición

Inicia el programa con los catálogos vacíos, entra a Administración y pulsa **Cargar demostración desde archivo**. El botón lee `datos-demostracion.csv` de la raíz del proyecto y crea:

- 20 clientes y una tarjeta por cada uno (`C-01` a `C-20`).
- Recargas de 10 millones de pesos por tarjeta.
- 10 habitaciones.
- 5 servicios individuales, un paquete y tres beneficios de puntos.
- 20 reservas, diez de ellas con servicios adicionales.
- 10 compras de servicios independientes de una reserva.
- Movimientos distribuidos en días distintos para los informes y cierres fiscales históricos.

Cliente de ejemplo: `cliente01@hoteluq.demo` / `cliente123`.

La precarga puede realizarse solo una vez por sesión, desde una ejecución limpia. Si se añaden registros manualmente antes de pulsar el botón, la precarga se bloquea ya que es necesario comenzar con el hotel vacío, para no mezclar dos conjuntos de datos.

## Funciones principales

- Registro y autenticación de clientes, tarjeta virtual con saldo y movimientos.
- Reserva de habitaciones con cotización, pagos en tarjeta y uso opcional de puntos.
- Compras múltiples sin reserva, incluidas compras con puntos.
- Administración mediante tablas de clientes, tarjetas, habitaciones, reservas, servicios, paquetes y promociones.
- Estadísticas, informes operativos, exportaciones y cierres fiscales.


### Alcance de las pruebas

Se compiló con Java 21 y JavaFX. 
