# Architecture, SOLID, and design-pattern evidence

## MVC and dependency direction

JavaFX nodes live under presentation.javafx; ReservationController handles UI actions and delegates to ReservationFacade. The facade depends on the ReservationUseCase contract. Application services depend on ports such as RoomCatalog, ReservationRepository, ServiceCatalog, and PaymentGateway; in-memory adapters implement those contracts under infrastructure. Domain models do not depend on JavaFX or storage.

## SOLID evidence

| Principle | Problem addressed | Evidence |
|---|---|---|---|
| SRP | One class handling UI, validation, pricing, storage, and payment changes for unrelated reasons. | ReservationController, AvailabilityService, AccommodationPricingService, ReservationApplicationService, and repositories have separate responsibilities. |
| OCP | Adding a room type or cancellation policy should not require rewriting the reservation workflow. | RoomFactory has concrete factories; CancellationPolicy is strategy-based; ServiceOption supports new options and packages. |
| LSP | Specialized room factories and service options must work through their common contracts. | StandardRoomFactory and SuiteRoomFactory implement the same creation contract; individual services and packages implement ServiceOption. |
| ISP | A large hotel interface would force modules to depend on methods they do not use. | Separate catalog, repository, payment, promotion, and loyalty ports expose narrow contracts. |
| DIP | Business rules tied to concrete in-memory storage or wallet classes are hard to replace. | ReservationApplicationService receives interfaces; InMemoryHotelComponentsFactory wires implementations at the composition root. |

## Pattern inventory

### Creational

| Pattern | Need | Classes | Scope |
|---|---|---|---|
| Factory Method | Create rooms by type while allowing new room types to add their own creation logic. | RoomFactory, StandardRoomFactory, SuiteRoomFactory, RoomFactoryRegistry. | Implemented: rooms/reservations. |
| Builder | Reservation requests have multiple required and optional values and should be validated by the use case. | ReservationRequest.Builder. | Implemented: reservations. |
| Abstract Factory | Assemble a consistent set of storage and payment adapters for the selected environment. | HotelComponentsFactory, InMemoryHotelComponentsFactory, HotelComponents. | Implemented: composition root. |

### Structural

| Pattern | Need | Classes | Scope |
|---|---|---|---|
| Composite | A service package can contain single services or nested packages and be priced as one option. | ServiceOption, AdditionalService, ServicePackage. | Implemented: reservations/services integration. |
| Decorator | Add service costs to an accommodation price without changing the room-price calculator. | PriceComponent, RoomStayPrice, ServicePriceDecorator. | Implemented: reservations/pricing. |
| Adapter | Present a stable application payment contract over the virtual-card wallet API. | PaymentGateway, VirtualCardWallet, VirtualCardPaymentAdapter. | Implemented: reservation payment boundary. |
| Facade | Give the JavaFX controller one API for availability, reservation, guest cancellation, and history. | ReservationFacade, ReservationUseCase. | Implemented: reservations/presentation. |

### Behavioral

| Pattern | Need | Classes | Scope |
|---|---|---|
| State | Allow only valid reservation transitions and make status behavior explicit. | ReservationState, PendingPaymentState, ConfirmedState, CancelledState, Reservation. | Implemented: reservations. |
| Strategy | Apply guest and hotel cancellation/refund policies without branching throughout the workflow. | CancellationPolicy, StandardCancellationPolicy, HotelCancellationPolicy. | Implemented: reservations/admin integration. |
| Observer | Record history and send notifications only after successful creation/cancellation. | ReservationEventPublisher, ReservationEventObserver, ActivityHistoryObserver, CustomerNotificationObserver. | Implemented: reservations; future modules can subscribe. |

## Extension contracts outside this branch’s implementation scope

- Customer identity and authentication: guest/admin account services.
- Room administration: room and room-type management use cases.
- Promotions: eligibility and discount strategies.
- Loyalty: points ledger, expiry, redemption, and benefits.
- Admin operations: card recharge authorization. Reservation code provides a separate `HotelReservationAdministrationUseCase` contract for hotel-initiated cancellation and its 100% refund policy; authorization belongs to the admin module.
- Reporting: reservation, occupancy, revenue, service, recharge, refund, and loyalty queries; TXT/PDF exporters and JavaFX charts.

These are integration points, not substitutes for teammates’ complete modules.
