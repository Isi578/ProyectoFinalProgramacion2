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

## Architecture

- domain: hotel entities, reservation lifecycle and pricing
- application: reservation use cases and contracts for other modules
- infrastructure: in-memory repositories and virtual-card adapter
- presentation.javafx: JavaFX MVC view and controller

See docs/requirements-analysis.md and docs/architecture-patterns.md for requirement mapping, proposed group rules, SOLID evidence, and design patterns.