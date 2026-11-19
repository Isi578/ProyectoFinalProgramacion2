# HotelUQ

HotelUQ is a JavaFX hotel-management project for Programming II. This branch contains the reservation vertical slice and integration contracts for the remaining hotel modules.

## Requirements

- JDK 25 or newer
- Maven 3.8.5 or newer
- IntelliJ IDEA with the project opened as a Maven project

JavaFX is resolved by Maven; no separate JavaFX SDK installation is needed.

## Run

From the project folder, run: mvn clean javafx:run

In IntelliJ, open pom.xml as a Maven project, select a JDK 25 project SDK, reload Maven, then run the javafx:run Maven goal.

## Reservation demo data

The application starts with sample rooms, a guest, a virtual card, and additional services. The screen lets you check room availability, create a reservation, cancel it, and inspect the event history.

Demo guest ID: GUEST-ANA-001
Demo card ID: CARD-ANA-001

Customer credentials, card balances, payment movements, reservations, and other demo data use in-memory repositories and are lost when the application restarts. The configured administrator ID is a demonstration allowlist, not secure user authentication.

## Customer account (login and registration)

The first tab, "Cuenta de cliente", lets a customer sign in or create an account. It uses `CustomerAccountUseCase` (PBKDF2 password hashing, unique email).

The Reservations tab is locked until a customer signs in, and locks again on sign-out. Reservations are always made, listed and cancelled on behalf of the signed-in customer: a customer cannot cancel another customer's reservation or see their activity events. The access rule lives in `ReservationAccessGate` and is shared by `HotelUqApplication` and `HotelUqApplication1`.

The reservation screen is in Spanish. Messages coming from the application and domain layers are translated in `ReservationMessages`; a message that is not in its list is shown as received.

Demo customer: ana@example.com (card CARD-ANA-001). Customers created from the registration form do not have a virtual card yet, so paying a reservation requires a card owned by that customer.

## Architecture

- domain: hotel entities, reservation lifecycle and pricing
- application: reservation use cases and contracts for other modules
- infrastructure: in-memory repositories and virtual-card adapter
- presentation.javafx: JavaFX MVC view and controller

See docs/requirements-analysis.md and docs/architecture-patterns.md for requirement mapping, proposed group rules, SOLID evidence, and design patterns.