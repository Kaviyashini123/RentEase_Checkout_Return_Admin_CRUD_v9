# RentEase — Restyled Frontend + Validation

This drop-in package restyles every existing Thymeleaf page in your
`rentease-system` project with a single dark "cyan/violet" design system
(inspired by the design tokens in your provided template) and adds
client-side form validation for every module. It does **not** change any
controller, service, or model code — every `th:field` / `name` attribute
matches what your existing controllers already expect, so nothing on the
backend needs to change.

## What's included

```
templates/
  fragments/layout.html      <- shared <head>, navbar, footer (new)
  search/search.html         <- restyled + min/max price validation
  search/details.html        <- restyled + safer favorites feedback
  booking/booking-form.html  <- restyled + date/ID validation
  booking/dashboard.html     <- restyled + cancellation-reason validation
  payment/checkout.html      <- restyled + card number/expiry/CVV validation
  payment/invoice.html       <- restyled
  fleet/manage-fleet.html    <- restyled
  fleet/add-vehicle.html     <- restyled + plate/date/value validation
  checkout/checkout-form.html<- restyled + handover validation
  checkout/return-form.html  <- restyled + return/damage validation
  reports/admin-dashboard.html <- restyled + export-period validation
  reports/review-form.html   <- restyled + rating validation
static/
  css/style.css              <- shared design system (replaces old style.css)
  js/validation.js           <- all client-side validation rules
```

## How to apply it

1. Copy the `templates/` and `static/` folders in this package **into**
   `rentease-system/src/main/resources/`, overwriting the existing files
   with the same names.
2. Rebuild/run as usual (`mvn spring-boot:run`). No pom.xml or Java changes
   are required.

## Design system

Dark canvas (`#0B0D12`) with card surfaces, a cyan→violet gradient for
primary actions (`--cyan #22D3EE`, `--violet #A78BFA`), soft glassy navbar,
rounded cards/tables/badges, and print-friendly styling for the invoice
page. All colors/spacing are CSS variables at the top of `style.css`, so
you can retheme quickly if a supervisor asks for a different palette.

## Validation added (client-side, per module)

1. **Vehicle Search & Browsing** — min price ≤ max price, no negative prices.
2. **Booking & Reservation Management** — customer/vehicle IDs must be
   positive; start date can't be in the past; end date must be after start
   date; cancellation reason required (min 5 characters).
3. **Payment & Invoicing** — card number (16 digits), expiry `MM/YY` must
   be a real, non-expired date, CVV must be exactly 3 digits — only
   enforced when the method is a card, not cash/mock gateway.
4. **Fleet & Vehicle Management** — brand/model/plate required, plate
   number pattern check, acquisition date can't be in the future, purchase
   value/odometer can't be negative, next service date can't be before
   acquisition date.
5. **Rental Checkout & Return Processing** — handover requires positive
   reservation/vehicle IDs, non-negative odometer, staff ID, and a future
   expected-return date/time; return requires odometer/staff ID and pairs
   damage description with severity (both required together).
6. **Reports & Reviews** — review requires a valid customer ID and a
   rating between 1–5; report export period must be `YYYY-MM`.

Every rule shows an inline red message under the offending field and
blocks submission until it's fixed — this is a **usability layer only**.
Your services (e.g. `BookingService`, `PaymentService`) still do the real
validation server-side, which is what actually protects your data.

## Suggested talking point for your viva / report

> "We implemented client-side validation using vanilla JavaScript
> (`validation.js`) attached per form, giving immediate feedback before a
> request reaches the server, while the existing Spring services continue
> to enforce the same rules server-side as the source of truth."

This maps directly onto the "Validations" and "CRUD Operations" rubric
rows for your Progress Evaluation.
