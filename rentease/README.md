# RentEase — Vehicle Rental Management System

RentEase is a Spring Boot 3.3.4 + Java 17 vehicle-rental web application using Thymeleaf, Spring Data JPA and Microsoft SQL Server Express.

## Module structure

The project is organized into **five functional modules**. Search is a feature inside Booking, and CRUD operations live inside the module that owns the corresponding data.

| Module | Package | Main responsibilities | Main CRUD/API area |
|---|---|---|---|
| **1. Booking** | `booking` | Vehicle search, filtering, vehicle details, favourites and vehicle catalog management | `/booking`, `/api/booking/vehicles` |
| **2. Reservation** | `reservation` | Create, view, update, confirm, reassign and cancel reservations | `/reservation`, `/api/reservation` |
| **3. Payment** | `payment` | Payment processing, invoices, refunds and discount codes | `/payment`, `/api/payment/payments` |
| **4. Checkout** | `checkout` | Rental handover, return processing and damage reports | `/checkout`, `/api/checkout/records` |
| **5. Reports** | `reports` | Revenue/vehicle-utilization reports, reviews and report-export records | `/reports`, `/api/reports` |

### Important structural changes

- The old **Search module** has been merged into **Booking**.
- The old **Booking & Reservation module** has been split so reservations now belong to the **Reservation module**.
- The **Fleet module has been removed**.
- The separate **Admin CRUD module/page has been removed**.
- CRUD endpoints are now located under the module that owns the data instead of under `/api/admin/...`.
- The admin/staff login is still available for staff-only workflows; it is not a separate CRUD-management module.

## Running the project

1. Open the `rentease-system` folder in IntelliJ IDEA or another Java IDE.
2. Make sure Java 17 and SQL Server Express are running.
3. Update `src/main/resources/application.properties` with the SQL Server credentials.
4. Run `RenteaseSystemApplication`.
5. Open `http://localhost:8080`.

The database schema is managed by Hibernate and the demo data is loaded from `data.sql`.
