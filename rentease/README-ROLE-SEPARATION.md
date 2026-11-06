# RentEase – Customer/Admin Role Separation

This backend update keeps the existing Spring Boot + Thymeleaf architecture and separates practical responsibilities between the two portals.

## Customer portal
- Customer login / registration
- Vehicle search, filtering and details
- Favorites
- Create a reservation/booking
- View only their own reservations
- Cancel their own reservation
- Customer payment checkout
- View their invoice after successful payment
- Submit vehicle reviews
- View public vehicle ratings

## Admin portal
- Admin login
- Vehicle catalogue CRUD
- Reservation oversight and administration
- Payment and invoice CRUD / management
- Discount-code management
- Rental checkout and return processing
- Damage/late-fee processing
- Reports dashboard and PDF export
- Review moderation/update/delete
- Report/export record management

## Important backend security changes
- Customer IDs are no longer trusted from booking/payment/review form data; the logged-in session identity is used.
- Customer reservation listings are scoped to the logged-in customer.
- Admin-only management endpoints are protected server-side, including API endpoints.
- Reservation administration mutations require an admin session.
- Report analytics/export and review moderation APIs require an admin session.
- Customer review submission remains available without exposing report administration.

## CRUD interpretation
CRUD remains available at the module/data-management level, but it is intentionally role-scoped. A customer should not receive unrestricted Create/Read/Update/Delete access to administrative records such as every customer's payments, vehicles, checkout records, or business reports.
