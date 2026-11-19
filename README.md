# HotelUQ — Sistema de gestión y reservas hoteleras

**Proyecto final · Programación II · Universidad del Quindío · período 2026-2**

HotelUQ es una aplicación de escritorio desarrollada en **Java 25 y JavaFX 27.0.1** para gestionar clientes, tarjetas virtuales, habitaciones, reservas, servicios adicionales, promociones, fidelización por puntos e informes administrativos y fiscales. Su diseño separa presentación, reglas de negocio y acceso a datos, y utiliza patrones de diseño para facilitar su extensión y mantenimiento.

> **Alcance académico:** aplicación funcional de demostración, con almacenamiento operativo en memoria y credenciales administrativas de prueba.

## 1. Objetivo y alcance

El objetivo es representar la operación básica de un hotel mediante una interfaz gráfica y reglas de negocio verificables. Los clientes pueden registrarse, consultar habitaciones, cotizar y pagar reservas, comprar servicios adicionales, usar una tarjeta virtual y administrar puntos de fidelización. La administración permite consultar y modificar catálogos, gestionar reservas y saldos, generar informes y efectuar cierres fiscales.

**Actores:**

- **Cliente:** se registra, inicia sesión, utiliza su tarjeta, consulta disponibilidad, reserva, compra servicios, canjea puntos y consulta su historial.
- **Administrador:** inicia sesión en el módulo de administración, gestiona habitaciones, servicios y promociones, recarga tarjetas, consulta registros y genera informes y cierres fiscales.
- **Sistema HotelUQ:** validar reglas, calcula precios, comprueba disponibilidad, registra movimientos, gestiona puntos y notifica eventos internos.

La aplicación utiliza **una ventana principal** con pestañas de Cuenta de cliente, Reservas, Tarjeta virtual, Puntos y beneficios, Compras y Administración. Dentro de Administración se muestran tablas de clientes, tarjetas, habitaciones, reservas, servicios y promociones, además de las secciones de estadísticas y fiscal diario.

## 2. Requisitos de instalación

| Herramienta | Versión / finalidad |
|---|---|
| **JDK** | **25** (configurado mediante `maven.compiler.release=25`) |
| **Apache Maven** | **3.8.5 o superior** |
| **JavaFX Controls** | **27.0.1**; Maven gestiona la dependencia |
| **OpenPDF** | **3.0.5**, exportación de informes PDF, lo gestiona Maven |

Si se utiliza IntelliJ IDEA, configure el **Project SDK** y el SDK utilizado por Maven como JDK 25. 

## 3. Instalación y ejecución

1. Clone o descarga el repositorio del proyecto.
2. Abra una terminal en la **carpeta que contiene `pom.xml`**.
3. Verifique que el archivo `datos-demostracion.csv` este en la caperta de proyecto.
4. Compile y ejecute las pruebas:

```bash
mvn clean test
```

5. Inicie la interfaz gráfica:

```bash
mvn clean javafx:run
```

**Importante:** el proyecto usa rutas relativas para localizar el CSV; por tanto, ejecute los comandos desde la raíz del proyecto, no desde `src/` ni desde otra carpeta. La clase principal declarada en Maven es `com.hoteluq.presentation.HotelUqApplication`.

### Inicio limpio

Por defecto, HotelUQ **no crea registros ficticios**: no hay clientes, tarjetas, habitaciones, reservas, servicios, paquetes, promociones ni beneficios de puntos al iniciar. Es necesario registrar información desde la interfaz o usar la precarga manual para demostrar el funcionamiento completo.

Los clientes y tarjetas creados durante la ejecución se almacenan ** en memoria**: al cerrar la aplicación se pierden. Los archivos fiscales, por el contrario, pueden permanecer almacenados en el equipo.

## 4. Usuarios y demostración

### Acceso administrativo de demostración

| Dato | Valor |
|---|---|
| Identificador | `admin` |
| Contraseña | `hoteluq123` |

El módulo administrativo exige ambos datos y **cierra su sesión al abandonar la pestaña principal Administración**. Al regresar, se deben introducir las credenciales nuevamente. 

### Precarga opcional desde un archivo externo al código

El archivo **`datos-demostracion.csv`** se encuentra al mismo nivel que `pom.xml`. El programa **no lo carga automáticamente**. Para utilizarlo:

1. Inicie HotelUQ con el hotel vacío.
2. Ingrese a **Administración** con las credenciales anteriores.
3. En **Resumen general**, pulse **Cargar demostración desde archivo**.
4. Consulte las tablas administrativas, realice reservas y compras, y genere informes de distintos períodos.

El CSV crea:

| Tipo de dato | Cantidad / configuración |
|---|---|
| Clientes ficticios | **20**, con códigos `C-01` a `C-20` |
| Tarjetas virtuales | **20**, una por cliente y con su mismo identificador |
| Recarga inicial | **10.000.000 COP** por tarjeta |
| Habitaciones | **10**: 301–307 estándar y 308–310 suite |
| Servicios individuales / productos | **5** |
| Paquetes | **1** (paquete bienestar) |
| Beneficios de puntos | **3** |
| Reservas | **20**, de ellas **10** con servicios adicionales |
| Compras sin reserva | **10** |
| Historial financiero | Movimientos repartidos en varias fechas para demostrar reportes y cierres |

**Cuenta de cliente de demostración:** `cliente01@hoteluq.demo` / `cliente123`. Las demás cuentas y contraseñas están definidas en el CSV. La precarga solo puede ejecutarse **una vez por sesión** y requiere un hotel sin registros previos, para evitar datos mezclados o duplicados.

**Catálogo del CSV:** desayuno, estacionamiento, acceso al spa, agua embotellada, kit de aseo y paquete bienestar. Las habitaciones y los servicios **sí se crean desde el CSV**, no se limitan a referenciar catálogos precargados.

## 5. Módulos del sistema

| Módulo | Operaciones relevantes |
|---|---|
| **Cuenta de cliente** | Registro, autenticación y consulta de sesión. Identificadores consecutivos `C-01`, `C-02`, etc. |
| **Tarjeta virtual** | Creación asociada a la cuenta, consulta de saldo, cargos, recargas y devoluciones. Una tarjeta por cliente, con el mismo ID. |
| **Habitaciones** | Registro, actualización, desactivación, tarifas, capacidades y disponibilidad según fechas. |
| **Reservas** | Búsqueda de disponibilidad, selección de habitación y servicios, cálculo previo del total, pago y cancelación. |
| **Servicios y compras** | Servicios, productos y paquetes; selección múltiple y compra independiente de una reserva. |
| **Promociones** | Configuración, validación y activación/desactivación de descuentos. |
| **Fidelización** | Acumulación, consulta, canje de beneficios y pago total o parcial con puntos según la operación. |
| **Administración** | Panel con tablas y operaciones sobre clientes, tarjetas, habitaciones, reservas, servicios y promociones. |
| **Estadísticas** | Ocho tipos de informes, visualizaciones y exportación TXT/PDF. |
| **Fiscal diario** | Consulta provisional del día actual y archivos históricos cerrados de solo lectura desde la aplicación. |

## 6. Requisitos funcionales y casos de uso

El análisis de requisitos del proyecto identifica las siguientes funcionalidades (véase [`docs/requirements-analysis.md`](docs/requirements-analysis.md)). La tabla indica dónde pueden comprobarse en el código **integrado**, no únicamente en la primera rama de desarrollo.

| ID | Requisito funcional | Clases o módulos relacionados |
|---|---|---|
| **RF-001** | Consultar tipos, características y tarifas de habitaciones. | `Room`, `RoomType`, `RoomCatalog` |
| **RF-002** | Definir fechas de entrada y salida. | `ReservationView`, `ReservationRequest` |
| **RF-003** | Consultar disponibilidad por período. | `AvailabilityService`, `ReservationFacade` |
| **RF-004** | Crear reservas válidas. | `ReservationApplicationService`, `Reservation` |
| **RF-005** | Incluir servicios y paquetes en el precio total. | `ServiceOption`, `ServicePackage`, `AccommodationPricingService` |
| **RF-006** | Aplicar promociones que cumplan sus condiciones. | `PromotionService`, `PromotionCondition` |
| **RF-007** | Verificar tarjeta activa, titular y saldo antes del cobro. | `PaymentGateway`, `PaymentService` |
| **RF-008** | Registrar cargos y devoluciones. | `PaymentMovement`, `VirtualCardPaymentAdapter` |
| **RF-009** | Cancelar reservas conforme a políticas de reembolso. | `CancellationPolicy`, `StandardCancellationPolicy`, `HotelCancellationPolicy` |
| **RF-010** | Consultar reservas e historial del cliente. | `ReservationFacade`, `ActivityHistoryObserver` |
| **RF-011** | Administrar habitaciones, servicios y promociones. | `AdminDashboardService`, `AdminController` |
| **RF-012** | Recargar tarjetas desde Administración. | `CardRechargeUseCase` |
| **RF-013** | Acumular y canjear puntos de fidelización. | `LoyaltyService`, `LoyaltyAccount` |
| **RF-014** | Generar informes y exportaciones. | `HotelReportService`, `ReportExporter`, `PdfReportExporter` |
| **RF-015** | Proveer datos suficientes para la demostración. | `DemoDataLoader`, `datos-demostracion.csv` (**carga manual**) |

### Casos de uso principales

| Caso | Actor | Flujo básico y resultado |
|---|---|---|
| **CU-01. Registrar cliente** | Cliente | Introduce datos válidos; se crea la cuenta y su tarjeta virtual. |
| **CU-02. Iniciar sesión** | Cliente | Se validan las credenciales y se habilitan las pestañas correspondientes. |
| **CU-03. Consultar disponibilidad** | Cliente | Indica fechas; el sistema devuelve habitaciones libres para ese intervalo. |
| **CU-04. Crear reserva** | Cliente | Elige habitación, servicios, tarjeta y puntos opcionales; consulta el total y confirma el cobro. |
| **CU-05. Cancelar reserva** | Cliente / administrador | Solicita cancelación; se valida el estado y se calcula la devolución aplicable. |
| **CU-06. Comprar servicios** | Cliente | Selecciona varios servicios o productos, consulta el total y paga con tarjeta y/o puntos. |
| **CU-07. Gestionar catálogo** | Administrador | Crea o modifica habitaciones, servicios, paquetes y promociones. |
| **CU-08. Recargar tarjeta** | Administrador | Selecciona tarjeta, introduce importe y se registra el movimiento. |
| **CU-09. Consultar informes** | Administrador | Selecciona tipo y período, obtiene resultados y exporta cuando corresponde. |
| **CU-10. Consultar cierre fiscal** | Administrador | Selecciona fecha y consulta el informe provisional o archivado. |
| **CU-11. Precargar demostración** | Administrador | Ejecuta manualmente la lectura y validación de `datos-demostracion.csv`. |

**Flujo de reserva:** fechas → disponibilidad → habitación y huéspedes → servicios opcionales → promoción y puntos opcionales → **calcular total** → validar tarjeta y saldo → cobrar → confirmar y registrar reserva. El sistema no confirma una reserva si sus validaciones o el pago fallan.

## 7. Reglas de negocio

Estas reglas están documentadas en el análisis del proyecto y, cuando corresponde, representadas en las clases indicadas.

1. **Disponibilidad:** no se permiten dos reservas confirmadas de una misma habitación para intervalos solapados. La salida de una reserva puede coincidir con la entrada de otra.
2. **Fechas:** la salida debe ser posterior a la entrada; las reservas ordinarias no deben comenzar en el pasado.
3. **Capacidad:** el número de huéspedes no puede exceder la capacidad de la habitación.
4. **Cotización:** el total se calcula con tarifa, cantidad de noches, servicios seleccionados y promoción válida; los puntos opcionales se descuentan antes del cobro monetario.
5. **Pago:** se requiere tarjeta activa, perteneciente al cliente y con saldo suficiente para el importe que se cobre en dinero.
6. **Confirmación:** la reserva se confirma únicamente después de realizar correctamente el cobro exigible.
7. **Servicios:** solo pueden contratarse opciones activas del catálogo, que pueden incluir paquetes.
8. **Recargas:** se realizan desde la administración y se registran como movimientos; **no son ingresos por hospedaje**.
9. **Cancelación solicitada por cliente:** más de **72 horas** antes de la entrada: devolución del **100 %**; entre **24 y 72 horas**, inclusive: **80 %**; menos de 24 horas: **0 %**.
10. **Cancelación realizada por el hotel:** devolución del **100 %** conforme a la política administrativa.
11. **Devoluciones:** se aplican a la tarjeta original y se evita reembolsar dos veces un mismo cargo; los puntos utilizados se restauran según la proporción de reembolso aplicable.
12. **Fidelización:** los puntos se acumulan con una estrategia de **1 punto por cada 500 COP de gasto elegible**. Los puntos expiran al año y se consumen priorizando los de vencimiento más próximo.
13. **Pago con puntos:** **1 punto equivale a 50 COP** de cobertura. Las reservas requieren un componente monetario positivo; las compras independientes pueden cubrirse completamente con puntos si se dispone de saldo suficiente.
14. **Promociones:** se comprueban sus condiciones de aplicación y se limita el descuento según las políticas configuradas; los bonos y las promociones no deben vulnerar la regla del porcentaje máximo establecida en `PromotionService`.
15. **Trazabilidad:** cargos, recargas y devoluciones se registran por separado; el valor cubierto con puntos no constituye ingreso de efectivo.
16. **Precarga:** los registros de ejemplo son exclusivamente optativos; no se importa el CSV al iniciar ni se permite duplicar una precarga en la misma sesión.

## 8. Arquitectura y principios SOLID

El proyecto separa la presentación de los casos de uso y del modelo de dominio, siguiendo una organización **MVC con puertos y adaptadores**:

```text
JavaFX (vistas y controladores)
             |
             v
Casos de uso y servicios de aplicación
             |
             v
Entidades y reglas de negocio
             ^
             | contratos (interfaces)
Infraestructura (repositorios en memoria y adaptadores)
```

- **Presentación:** `presentation/`, con `ReservationView`, `ReservationController`, `AdminView` y sus controladores.
- **Aplicación:** `application/`, con reservas, disponibilidad, fidelización, pagos, gestión y reportes.
- **Dominio:** `domain/`, con habitaciones, clientes, servicios, reservas, estados y puntos.
- **Infraestructura:** `infrastructure/`, con repositorios en memoria, implementación de pagos y fábricas.
- **Informes:** `reporting/`, con datos, decoradores y exportadores TXT/PDF.

### Evidencia de los cinco principios SOLID

| Principio | Problema de diseño | Solución y clases involucradas | Por qué representa el principio |
|---|---|---|---|
| **SRP — Responsabilidad única** | Un controlador que calcule tarifas, valide disponibilidad, cobre y guarde datos tendría demasiadas razones para cambiar. | `ReservationController` coordina la vista; `AvailabilityService` evalúa ocupación; `AccommodationPricingService` calcula precios; `ReservationApplicationService` orquesta el caso de uso; repositorios guardan entidades. | Cada componente tiene una responsabilidad predominante y los cambios de UI no obligan a reescribir las reglas de pagos. |
| **OCP — Abierto/cerrado** | Incorporar otro tipo de habitación o política de cancelación no debería requerir cambiar el algoritmo principal de reserva. | `RoomFactory` con `StandardRoomFactory` y `SuiteRoomFactory`; `CancellationPolicy` con `StandardCancellationPolicy` y `HotelCancellationPolicy`; `ServiceOption` admite servicios o paquetes. | Se agregan implementaciones de contratos sin alterar indiscriminadamente el código consumidor. |
| **LSP — Sustitución de Liskov** | Un servicio o una fábrica especializada no debería romper a los clientes que esperan el contrato general. | `AdditionalService` y `ServicePackage` implementan `ServiceOption`; las dos fábricas de habitación implementan `RoomFactory`. | Los consumidores trabajan con los contratos y no deben conocer la clase concreta para consultar precio o crear habitaciones. |
| **ISP — Segregación de interfaces** | Una interfaz única de todas las operaciones obligaría a módulos distintos a depender de métodos ajenos. | `RoomCatalog`, `ReservationRepository`, `PaymentGateway`, `ServiceCatalog`, `LoyaltyPointsUseCase` y otros puertos específicos. | Cada caso de uso depende solo de las operaciones que necesita. |
| **DIP — Inversión de dependencias** | La lógica de reservas quedaría acoplada a repositorios en memoria o a una implementación particular de pagos. | `ReservationApplicationService` depende de puertos (`RoomCatalog`, `ReservationRepository`, `PaymentGateway`, etc.); `InMemoryHotelComponentsFactory` conecta implementaciones concretas. | Se pueden sustituir adaptadores sin trasladar el conocimiento de infraestructura al dominio. |

## 9. Patrones de diseño implementados

### 9.1. Creacionales (3)

| Patrón | Problema y decisión | Clases y evidencia de implementación |
|---|---|---|
| **Factory Method** | Crear una habitación según su tipo sin dispersar condicionales de construcción por las reservas. Se encapsula la creación tras una fábrica extensible. | `RoomFactory`, `StandardRoomFactory`, `SuiteRoomFactory`, `RoomFactoryRegistry`. Las fábricas se registran en `InMemoryHotelComponentsFactory`. |
| **Builder** | Una solicitud de reserva combina datos obligatorios y opcionales; un constructor largo sería poco legible. Se construye paso a paso y se valida en el caso de uso. | `ReservationRequest.Builder` y `ReservationRequest`; lo utiliza el flujo de `ReservationApplicationService`. |
| **Abstract Factory** | La aplicación requiere un conjunto coherente de repositorios y servicios conectados entre sí. Una fábrica concreta ensambla todos los componentes del entorno en memoria. | `HotelComponentsFactory`, `InMemoryHotelComponentsFactory`, `HotelComponents`; la composición se invoca desde `HotelUqApplication`. |


### 9.2. Estructurales (4)

| Patrón | Problema y decisión | Clases y evidencia de implementación |
|---|---|---|
| **Composite** | Una compra o reserva debe tratar igual un servicio único y un paquete compuesto. Ambos comparten contrato y pueden calcularse desde la misma operación. | `ServiceOption`, `AdditionalService`, `ServicePackage`; los paquetes contienen opciones y se consultan mediante `ServiceCatalog`. |
| **Decorator** | El costo de servicios opcionales se añade al alojamiento sin cambiar su precio base ni crear clases para cada combinación. | `PriceComponent`, `RoomStayPrice`, `ServicePriceDecorator`; se utiliza durante el cálculo de costos. |
| **Adapter** | El servicio de reservas conoce `PaymentGateway`, mientras que el módulo de tarjeta virtual ofrece otro contrato. Un adaptador une ambos sin acoplar sus interfaces. | `PaymentGateway`, `VirtualCardPaymentPort`, `VirtualCardPaymentAdapter`; creado en `InMemoryHotelComponentsFactory`. |
| **Facade** | La interfaz no debería coordinar directamente catálogos, disponibilidad, pagos y repositorios. Se expone una operación simplificada para el flujo de reservas. | `ReservationFacade`, `ReservationUseCase`, `ReservationController`; la vista delega el caso de uso. |

### 9.3. De comportamiento (3)

| Patrón | Problema y decisión | Clases y evidencia de implementación |
|---|---|---|
| **State** | Una reserva pendiente, confirmada o cancelada no permite las mismas operaciones. Las transiciones se modelan mediante estados. | `ReservationState`, `PendingPaymentState`, `ConfirmedState`, `CancelledState`, `Reservation`; se usan al confirmar y cancelar. |
| **Strategy** | Los porcentajes de devolución dependen de quién cancela y del tiempo de anticipación. Se separa cada algoritmo de la coordinación del caso de uso. | `CancellationPolicy`, `StandardCancellationPolicy`, `HotelCancellationPolicy`; son invocadas por el servicio de reservas. |
| **Observer** | Los eventos de creación y cancelación deben reflejarse en actividad y notificaciones sin añadir ese trabajo al modelo de reserva. | `ReservationEventPublisher`, `ReservationEventObserver`, `ActivityHistoryObserver`, `CustomerNotificationObserver`; suscriptores reaccionan a eventos. |

## 10. Informes, gráficas y cierres fiscales

### Informes disponibles (8)

| Tipo | Finalidad |
|---|---|
| Reservas por período | Consultar reservas en un rango de fechas. |
| Habitaciones más reservadas | Comparar frecuencia de reservas por habitación. |
| Ocupación | Estimar ocupación de habitaciones según el período seleccionado. |
| Ingresos por período | Mostrar movimientos monetarios de cobro y su comportamiento temporal. |
| Servicios adicionales | Mostrar contratación de servicios en reservas y compras independientes. |
| Recargas | Consultar ingresos de saldo a las tarjetas virtuales (**no equivalen a ventas**). |
| Cancelaciones y devoluciones | Consultar cancelaciones y reembolsos registrados. |
| Puntos de clientes | Consultar información de fidelización y movimientos de puntos. |

**Exportación:** los informes operativos cuentan con opciones **TXT** y **PDF** mediante `TextReportExporter` y `PdfReportExporter`. La sección de estadísticas también ofrece representaciones gráficas. Los filtros de fecha permiten demostrar operaciones de varios días.

### Fiscal diario

`DailyFiscalService` calcula el resumen diario de reservas y movimientos e incluye cargos, devoluciones, ingreso neto y recargas por separado. El **día actual es provisional**; los cierres históricos archivados se consultan en **solo lectura** desde la aplicación y no se sobrescriben.

Los cierres se guardan fuera de la carpeta del proyecto, en:

```text
~/.hoteluq/fiscales/
```

**Para probar la versión de entrega:**

```bash
mvn clean test
mvn clean javafx:run
```

## 11. Organización del repositorio y aportes

```text
ProyectoFinalProgramacion2-main/
├── pom.xml                       # Dependencias, compilación y ejecución
├── README.md                     # Documentación principal
├── datos-demostracion.csv        # Datos de muestra; carga opcional
├── src/
│   ├── main/java/com/hoteluq/
│   │   ├── presentation/         # Interfaz gráfica y controladores
│   │   ├── application/          # Casos de uso y servicios
│   │   ├── domain/               # Entidades, estados y reglas
│   │   ├── infrastructure/       # Fábricas y repositorios en memoria
│   │   └── reporting/            # Contenido y exportación de informes
│   └── test/java/               # Pruebas automatizadas
└── LICENSE
```

### Miembros del grupo

- **Miguel Bocanegra**
- **Jhoan Avilés**
- **Isabela**
- **David**

**Repositorio del equipo:** _[https://github.com/Isi578/ProyectoFinalProgramacion2]_  
