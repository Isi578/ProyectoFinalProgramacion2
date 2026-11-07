# HotelUQ requirements analysis

## Document interpretation and scope

The brief describes HotelUQ, a hotel management system, although some page headers say “Plataforma de Logística.” The hotel context, actors, and business rules are treated as the operative requirements. The brief requires Java, JavaFX, MVC, domain exceptions, seed data, reports, a class diagram PDF, SOLID evidence, and at least 3 creational, 4 structural, and 3 behavioral patterns.

This branch implements the reservation flow and the in-memory contracts/adapters it needs. Authentication, administrator screens, full room administration, production promotions, points redemption, reporting, and export remain integration work for the rest of the team.

## Actors

- Guest / customer: browses rooms, checks availability, creates and cancels their own reservations, and views reservation history.
- Administrator: manages hotel data, may cancel a reservation on behalf of the hotel, recharges cards, and views reports.
- HotelUQ application: checks availability, computes totals, delegates card payments/refunds, and publishes activity events.

## Functional requirements

| ID | Requirement | Module and representative classes/contracts |
|---|---|---|
| RF-001 | Browse room types, descriptions, and nightly rates. | Rooms: `RoomCatalog`, `InMemoryRoomCatalog`, `Room`, `RoomType`. |
| RF-002 | Select check-in and check-out dates. | Reservations/UI: `ReservationRequest`, `ReservationView`, `ReservationController`. |
| RF-003 | Find available rooms for a stay. | Reservations: `AvailabilityService`, `ReservationUseCase`, `ReservationRepository`. |
| RF-004 | Create a reservation for a guest and room. | Reservations: `ReservationApplicationService`, `Reservation`, `ReservationUseCase`. |
| RF-005 | Include available additional services and packages in a reservation total. | Services/pricing: `ServiceCatalog`, `ServiceOption`, `ServicePackage`, `AccommodationPricingService`, `ServicePriceDecorator`. |
| RF-006 | Apply promotions only when requirements are satisfied. | Promotions: `PromotionPolicy`, `NoPromotionPolicy`; promotion eligibility rules remain group-defined. |
| RF-007 | Require an active card with enough funds before confirming. | Reservations/payments boundary: `PaymentGateway`, `VirtualCardPaymentPort`, `VirtualCardPaymentAdapter`; `InMemoryVirtualCardPaymentPort` is demo-only. |
| RF-008 | Record card charges and return refunds to the card used for payment. | Payment integration: `PaymentReceipt`, `PaymentMovement`; the card module implements `VirtualCardPaymentPort` in the full system. |
| RF-009 | Cancel a reservation and calculate the refund from time to arrival. | Reservations/admin: `CancellationPolicy`, `StandardCancellationPolicy`, `HotelCancellationPolicy`, `HotelReservationAdministrationUseCase`. |
| RF-010 | View a guest’s reservation history and activity. | Reservations: `ReservationUseCase`, `ReservationRepository`, `ActivityHistoryObserver`. |
| RF-011 | Administer rooms, room types, customers, additional services, and promotions. | Integration contracts: `RoomAdministrationUseCase`, `CustomerAccountUseCase`, `ServiceAdministrationUseCase`, `PromotionAdministrationUseCase`; full modules remain outside this branch’s scope. |
| RF-012 | Recharge virtual cards only as an administrator. | Payments/admin: `CardRechargeUseCase` contract; recharge logic and authorization belong to the card/admin modules. No recharge operation exists in the reservation demo adapter. |
| RF-013 | Accumulate and redeem points, expiring them after one year and spending earliest-expiring points first. | Loyalty: `LoyaltyPointsPort`, `LoyaltyPointsUseCase`; the ledger belongs to the loyalty module. |
| RF-014 | Generate operational reports as TXT/PDF and visual charts. | Reporting: `HotelReportQuery`, `ReportExporter`; full queries, exporters, and charts remain team work. |
| RF-015 | Include enough initial data to demonstrate the application. | Composition/in-memory data: `InMemoryHotelComponentsFactory`, in-memory catalogs, and a seeded demo payment port. |

## Business rules

- BR-001: Every room belongs to one room type.
- BR-002: A room cannot have two confirmed reservations whose stay intervals overlap. Intervals are treated as [check-in, check-out), so one guest may check out on the same date another checks in.
- BR-003: Check-out must be later than check-in.
- BR-004: A reservation total includes room-type nightly rate multiplied by nights plus selected services, then any valid promotion.
- BR-005: Availability is checked before payment and confirmation.
- BR-006: A confirmed reservation retains its guest, room, stay dates, selected services, original payment, and paid total.
- BR-007: The card must belong to the guest, be active, and have sufficient balance before confirmation.
- BR-008: An approved charge decreases card balance and creates a ledger movement.
- BR-009: A card recharge is an administrator action and must create a ledger movement.
- BR-010: Guest cancellation refund: more than 72 hours before check-in = 100%; 24 through 72 hours inclusive = 80%; less than 24 hours = 0%.
- BR-011: A hotel-initiated cancellation refunds 100%.
- BR-012: Refunds go to the original payment card and the same payment can be refunded at most once.
- BR-013: Additional services must be active/available and included in the total.
- BR-014: Promotion eligibility is verified by the promotion module before discounting.
- BR-015: Points expire one year after earning; redemptions use the soonest-to-expire available points first.

## Proposed additional rules for group confirmation

The brief asks the group to define at least two additional rules. These are proposals, implemented as baseline validation, and should be confirmed by the team:

- BR-A01: A reservation cannot start in the past.
- BR-A02: The number of guests cannot exceed the selected room’s capacity.

The demo form uses 15:00 check-in and 11:00 check-out defaults so cancellation windows can be calculated in hours. These are configurable UI defaults, not values stated in the brief.

## Main reservation use cases

1. Guest selects stay dates and requests availability.
2. Guest selects a room, guest count, available services, payment card, and optional promotion.
3. System validates the request and room capacity.
4. System checks that no confirmed reservation overlaps the requested interval.
5. System calculates the room and service subtotal, then asks the promotion policy for any valid discount.
6. System charges the active card. Only after successful payment does it confirm and persist the reservation.
7. System publishes a creation event for the activity history and notification observer.
8. Guest or administrator requests cancellation.
9. System chooses the applicable cancellation strategy, calculates the refund, refunds the original card at most once, changes reservation state, and publishes a cancellation event.

## Domain relationships

- `Room` (1) has exactly one `RoomType`; each `RoomType` (1) may describe zero or many rooms. This is an association, not composition, because the type is a shared catalog entry.
- `Reservation` (1) references exactly one `Customer` and one `Room`; each customer and room may be referenced by zero or many reservations over time. A room's reservations are exclusive for confirmed overlapping intervals.
- `Reservation` (1) contains zero or many selected `ServiceOption` values, and each option may be selected by many reservations. The reservation stores an immutable list snapshot so later catalog changes do not rewrite the booked selection.
- `ServicePackage` (1) contains one or many `ServiceOption` components; a component can be shared by multiple packages. This is Composite through a common contract, modeled as aggregation rather than ownership composition.
- A confirmed `Reservation` (1) retains exactly one `PaymentReceipt`; one charge receipt is associated with one reservation. Refund movements refer back to that original charge.
- `Reservation` (1) has exactly one current `ReservationState` at a time, replaced by a valid transition from pending payment to confirmed to cancelled.
- `ReservationEventPublisher` (1) notifies zero or many observers for an event. Observers do not own the reservation.
- Composition: a `Reservation` owns exactly one current `ReservationState` object; the state has no independent lifecycle in this model. Aggregation: reservations select reusable catalog `ServiceOption` objects, and `ServicePackage` groups service options that can also be reused by other packages.

| Entity / service | Responsibility | Main relationships and multiplicity |
|---|---|---|
| `Customer` | Customer identity used by reservations and payment ownership validation. | One customer to `0..*` reservations. |
| `RoomType` | Room category and nightly rate. | One type to `0..*` rooms. |
| `Room` | Room number, capacity, active status, and type. | Each room has exactly `1` type and `0..*` reservation records over time. |
| `Reservation` | Immutable booking facts plus lifecycle state. | Exactly `1` customer, `1` room, `1` payment receipt; `0..*` selected service options. |
| `ServiceOption` | Common pricing and availability contract for single services/packages. | A package aggregates `1..*` options; a reservation selects `0..*`. |
| `PaymentReceipt` | Snapshot of the successful original charge. | Exactly `1` per confirmed reservation; refund references its charge identifier. |
| `ReservationApplicationService` | Coordinates availability, pricing, promotion validation, payment, persistence, cancellation, and event publication. | Depends on catalog, repository, payment, and promotion ports. |

The brief does not specify timezone, same-day partial-night pricing, taxes, or promotion stacking. This implementation uses `LocalDateTime` in the system-local timezone, counts calendar nights, and delegates promotion rules to `PromotionPolicy`; these decisions should be confirmed by the group before production use.
## Additional implementation notes

- BR-A03 (proposal for team confirmation): a guest cannot cancel after check-in has started; an administrator may still cancel under the hotel policy.
- The demo form uses 15:00 check-in and 11:00 check-out defaults so cancellation windows can be calculated in hours. These configurable defaults are not values stated in the brief.
- Additional-service pricing units are configurable as per stay, per night, per guest, or per guest-night. Demo values are examples only and should be confirmed by the group.
