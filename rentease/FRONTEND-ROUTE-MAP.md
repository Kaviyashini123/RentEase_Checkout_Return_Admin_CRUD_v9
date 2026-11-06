# RentEase frontend route map

Frontend work is aligned to the routes and role checks already present in the supplied application. The five business modules are:

| Module | Customer experience | Staff operations | Templates |
| --- | --- | --- | --- |
| Fleet & vehicle discovery | `GET /booking`, vehicle details at `GET /booking/{id}`, save a favorite through `/api/booking/favorites` | `GET /booking/manage`, `/booking/new`, `/booking/edit/{id}`; matching POST create, edit and delete routes | `booking/search`, `booking/details`, `booking/manage`, `booking/vehicle-form` |
| Reservations | `GET /reservation/dashboard`, `/reservation/new`, POST `/reservation/new`, POST `/reservation/{id}/cancel` (owned reservations only) | Shared dashboard; staff-only edit and delete routes | `reservation/dashboard`, `reservation/reservation-form` |
| Payments & invoices | `GET/POST /payment/checkout`, `GET /payment/invoice/{invoiceId}` | `/payment/manage`, `/payment/new`, `/payment/edit/{id}`; matching POST create, edit and delete routes | `payment/checkout`, `payment/invoice`, `payment/manage`, `payment/payment-form` |
| Vehicle operations (handover, return, damage) | No customer page is exposed by the current role gate | `/checkout/handover`, `/checkout/return`, `/checkout/manage`, `/checkout/new`, `/checkout/edit/{id}`; matching POST operations | `checkout/checkout-form`, `checkout/return-form`, `checkout/manage`, `checkout/record-form` |
| Reviews & reporting | `GET/POST /reports/review`; completed reservations link to the supported review form | `/reports/dashboard`, `/reports/reviews`, review edit/delete, report export | `reports/review-form`, `reports/reviews`, `reports/reports-dashboard` |

Authentication remains at `/login`, `/admin/login`, `/register`, and `/logout`. Staff-only route enforcement remains in the existing interceptor.

## Existing integration limits

- The reservation list does not include a vehicle daily rate, and the payment checkout route requires one to calculate its total. The customer reservation screen therefore does not claim payment is available from a reservation until the backend exposes a trustworthy rate for that reservation.
- The current customer-facing routes support saving a favorite and submitting a review, but do not render a customer-specific favorites or review-history page.
- The vehicle seed records currently reference remote Unsplash images. The redesign reuses the existing vehicle image values and does not add new external image dependencies.
