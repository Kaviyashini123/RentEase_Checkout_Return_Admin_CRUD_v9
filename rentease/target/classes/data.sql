-- =============================================================================
-- RentEase — sample/demo data
-- =============================================================================
-- Spring Boot loads this file automatically on every startup (see the two
-- properties added in application.properties: spring.jpa.defer-datasource
-- -initialization=true and spring.sql.init.mode=always). Hibernate creates
-- the tables first (ddl-auto=update), then this script runs and fills them.
--
-- The sample rows are loaded into SQL Server on first startup. Each table is
-- guarded with IF NOT EXISTS so restarting the application does not duplicate
-- the seeded records.
--
-- Table/column names below match Hibernate's default snake_case mapping of
-- the entity fields (e.g. Vehicle.dailyRate -> vehicle.daily_rate).
-- Admin/customer accounts are NOT inserted here — those are still created by
-- AuthDataSeeder.java (Java) on first run: admin@rentease.lk / user@rentease.lk.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. VEHICLES (Booking module) — this is what /booking reads
-- -----------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM vehicle)
BEGIN
    SET IDENTITY_INSERT vehicle ON;
    INSERT INTO vehicle (id, brand, model, manufacture_year, plate_number, type, transmission,
                           fuel_type, seating_capacity, daily_rate, location, status,
                           description, image_url) VALUES
    (1,  'Toyota',   'Corolla Axio',   2021, 'CAB-1234', 'CAR',      'AUTOMATIC', 'PETROL',  5, 8500.00,  'Colombo',   'AVAILABLE',
         'Reliable, fuel-efficient sedan — great for city commutes.', 'https://images.unsplash.com/photo-1590362891991-f776e747a588'),
    (2,  'Honda',    'Vezel',          2022, 'CAC-4521', 'SUV',      'AUTOMATIC', 'HYBRID',  5, 13500.00, 'Colombo',   'AVAILABLE',
         'Compact hybrid SUV with great mileage and boot space.', 'https://images.unsplash.com/photo-1568605114967-8130f3a36994'),
    (3,  'Suzuki',   'Wagon R',        2020, 'CAA-7789', 'CAR',      'AUTOMATIC', 'PETROL',  4, 6000.00,  'Negombo',   'AVAILABLE',
         'Small and easy to park — ideal for solo travellers.', 'https://images.unsplash.com/photo-1541899481282-d53bffe3c35d'),
    (4,  'Toyota',   'HiAce',          2019, 'CAD-3345', 'VAN',      'MANUAL',    'DIESEL',  12, 15000.00, 'Kandy',     'AVAILABLE',
         'High-roof van, seats 12 — popular for group tours.', 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf'),
    (5,  'BMW',      '5 Series',       2023, 'CBA-9911', 'LUXURY',   'AUTOMATIC', 'PETROL',  5, 35000.00, 'Colombo',   'AVAILABLE',
         'Executive sedan for weddings and corporate travel.', 'https://images.unsplash.com/photo-1555215695-3004980ad54e'),
    (6,  'Yamaha',   'FZ-S',           2022, 'BAF-2210', 'BIKE',     'MANUAL',    'PETROL',  2, 2500.00,  'Galle',     'AVAILABLE',
         'Nimble commuter bike, great fuel economy.', 'https://images.unsplash.com/photo-1558981806-ec527fa84c39'),
    (7,  'Nissan',   'Caravan',        2020, 'CAE-6602', 'MINI_BUS', 'MANUAL',    'DIESEL',  15, 18000.00, 'Kurunegala','AVAILABLE',
         'Mini-bus for larger groups and staff transport.', 'https://images.unsplash.com/photo-1601362840469-51e4d8d58785'),
    (8,  'Toyota',   'Prius',          2021, 'CAF-1180', 'CAR',      'AUTOMATIC', 'HYBRID',  5, 11000.00, 'Colombo',   'AVAILABLE',
         'Hybrid sedan, very low fuel cost for long trips.', 'https://images.unsplash.com/photo-1621007947382-bb3c3994e3fb'),
    (9,  'Tesla',    'Model 3',        2023, 'CBB-4477', 'LUXURY',   'AUTOMATIC', 'ELECTRIC',5, 28000.00, 'Colombo',   'AVAILABLE',
         'Fully electric — instant torque, zero tailpipe emissions.', 'https://images.unsplash.com/photo-1554744512-d6c603f8c995'),
    (10, 'Mitsubishi','Montero Sport', 2021, 'CAG-8834', 'SUV',      'AUTOMATIC', 'DIESEL',  7, 17500.00, 'Kandy',     'BOOKED',
         '7-seater SUV, good for hill-country trips.', 'https://images.unsplash.com/photo-1519641471654-76ce0107ad1b'),
    (11, 'Honda',    'Dio',            2022, 'BAG-5567', 'BIKE',     'AUTOMATIC', 'PETROL',  2, 2200.00,  'Galle',     'UNDER_MAINTENANCE',
         'Automatic scooter, easy to ride, currently being serviced.', 'https://images.unsplash.com/photo-1609630875171-b1321377ee65'),
    (12, 'Toyota',   'Land Cruiser',   2018, 'CAH-2298', 'SUV',      'AUTOMATIC', 'DIESEL',  7, 32000.00, 'Colombo',   'INACTIVE',
         'Full-size 4x4 — temporarily withdrawn from the fleet.', 'https://images.unsplash.com/photo-1519641471654-76ce0107ad1b');
    SET IDENTITY_INSERT vehicle OFF;
END@@
-- -----------------------------------------------------------------------------
-- 2. RESERVATIONS (Reservation module) — customer_id 2 is the seeded demo customer
-- -----------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM reservations)
BEGIN
    SET IDENTITY_INSERT reservations ON;
    INSERT INTO reservations (id, customer_id, vehicle_id, start_date, end_date, estimated_cost,
                              status, pickup_location, dropoff_location, cancellation_reason,
                              created_at, updated_at) VALUES
    (1, 2, 1,  '2026-09-05', '2026-09-08', 25500.00,  'COMPLETED', 'Colombo', 'Colombo', NULL,
         '2026-09-01 09:15:00', '2026-09-08 17:00:00'),
    (2, 2, 10, '2026-09-20', '2026-09-25', 87500.00,  'CONFIRMED', 'Kandy',   'Kandy',   NULL,
         '2026-09-14 11:30:00', '2026-09-14 11:45:00'),
    (3, 2, 5,  '2026-08-10', '2026-08-11', 35000.00,  'CANCELLED', 'Colombo', 'Colombo', 'Change of travel plans',
         '2026-08-05 10:00:00', '2026-08-06 08:20:00'),
    (4, 2, 9,  '2026-09-22', '2026-09-24', 56000.00,  'PENDING',   'Colombo', 'Colombo', NULL,
         '2026-09-17 20:05:00', '2026-09-17 20:05:00');
    SET IDENTITY_INSERT reservations OFF;
END@@

-- Dedicated review-flow fixture for the seeded demo customer (customer_id 2).
-- Its dates are in the past relative to the current demo calendar, so COMPLETED
-- is a realistic review-eligible status. The natural-key guard keeps restarts safe.
IF NOT EXISTS (
    SELECT 1 FROM reservations
    WHERE customer_id = 2
      AND vehicle_id = 5
      AND start_date = '2026-10-01'
      AND end_date = '2026-10-03'
      AND status = 'COMPLETED'
)
BEGIN
    INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, estimated_cost,
                              status, pickup_location, dropoff_location, cancellation_reason,
                              created_at, updated_at)
    VALUES (2, 5, '2026-10-01', '2026-10-03', 70000.00,
            'COMPLETED', 'Colombo Airport', 'Kandy', NULL,
            '2026-10-01 08:30:00', '2026-10-03 17:00:00');
END@@

-- Additional review-flow fixtures for the seeded demo customer (customer_id 2).
-- Each natural-key guard makes these safe to load repeatedly. Vehicle IDs match
-- existing seed vehicles; estimated totals use their configured daily rates.
IF NOT EXISTS (
    SELECT 1 FROM reservations
    WHERE customer_id = 2 AND vehicle_id = 9
      AND start_date = '2026-08-18' AND end_date = '2026-08-21'
      AND status = 'COMPLETED'
)
BEGIN
    INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, estimated_cost,
                              status, pickup_location, dropoff_location, cancellation_reason,
                              created_at, updated_at)
    VALUES (2, 9, '2026-08-18', '2026-08-21', 84000.00,
            'COMPLETED', 'Colombo', 'Negombo', NULL,
            '2026-08-18 08:30:00', '2026-08-21 17:00:00');
END@@

IF NOT EXISTS (
    SELECT 1 FROM reservations
    WHERE customer_id = 2 AND vehicle_id = 2
      AND start_date = '2026-09-05' AND end_date = '2026-09-08'
      AND status = 'COMPLETED'
)
BEGIN
    INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, estimated_cost,
                              status, pickup_location, dropoff_location, cancellation_reason,
                              created_at, updated_at)
    VALUES (2, 2, '2026-09-05', '2026-09-08', 40500.00,
            'COMPLETED', 'Colombo Airport', 'Kandy', NULL,
            '2026-09-05 08:30:00', '2026-09-08 17:00:00');
END@@

IF NOT EXISTS (
    SELECT 1 FROM reservations
    WHERE customer_id = 2 AND vehicle_id = 2
      AND start_date = '2026-09-12' AND end_date = '2026-09-16'
      AND status = 'COMPLETED'
)
BEGIN
    INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, estimated_cost,
                              status, pickup_location, dropoff_location, cancellation_reason,
                              created_at, updated_at)
    VALUES (2, 2, '2026-09-12', '2026-09-16', 54000.00,
            'COMPLETED', 'Kandy', 'Colombo', NULL,
            '2026-09-12 08:30:00', '2026-09-16 17:00:00');
END@@

IF NOT EXISTS (
    SELECT 1 FROM reservations
    WHERE customer_id = 2 AND vehicle_id = 8
      AND start_date = '2026-09-20' AND end_date = '2026-09-23'
      AND status = 'COMPLETED'
)
BEGIN
    INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, estimated_cost,
                              status, pickup_location, dropoff_location, cancellation_reason,
                              created_at, updated_at)
    VALUES (2, 8, '2026-09-20', '2026-09-23', 33000.00,
            'COMPLETED', 'Colombo', 'Negombo', NULL,
            '2026-09-20 08:30:00', '2026-09-23 17:00:00');
END@@
-- Older local databases may not contain every vehicle from the starter seed.
-- Retarget only these review fixtures to real vehicles present in that case;
-- keep their reservation IDs, dates, status, routes and eligibility unchanged.
IF NOT EXISTS (SELECT 1 FROM vehicle WHERE id = 10)
BEGIN
    UPDATE reservations SET vehicle_id = 2, estimated_cost = 54000.00
    WHERE customer_id = 2 AND vehicle_id = 10
      AND start_date = '2026-09-12' AND end_date = '2026-09-16'
      AND status = 'COMPLETED';
END@@
IF NOT EXISTS (SELECT 1 FROM vehicle WHERE id = 1)
BEGIN
    UPDATE reservations SET vehicle_id = 8, estimated_cost = 33000.00
    WHERE customer_id = 2 AND vehicle_id = 1
      AND start_date = '2026-09-20' AND end_date = '2026-09-23'
      AND status = 'COMPLETED';
END@@
-- Confirmed but unpaid journey for testing the customer payment hand-off.
-- The natural-key check keeps this fixture idempotent across application restarts.
IF NOT EXISTS (
    SELECT 1 FROM reservations
    WHERE customer_id = 2 AND vehicle_id = 5
      AND start_date = '2026-11-10' AND end_date = '2026-11-12'
      AND status = 'CONFIRMED'
)
BEGIN
    INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, estimated_cost,
                              status, pickup_location, dropoff_location, cancellation_reason,
                              created_at, updated_at)
    VALUES (2, 5, '2026-11-10', '2026-11-12', 70000.00,
            'CONFIRMED', 'Colombo', 'Kandy', NULL,
            '2026-10-08 09:00:00', '2026-10-08 09:00:00');
END@@
-- Confirmed paid journey for testing the paid My Trips state and invoice view.
-- The reservation, payment and invoice are linked by stable natural keys so this
-- demonstration fixture is safe to load repeatedly.
IF NOT EXISTS (
    SELECT 1 FROM reservations
    WHERE customer_id = 2 AND vehicle_id = 9
      AND start_date = '2026-11-17' AND end_date = '2026-11-19'
      AND status = 'CONFIRMED'
)
BEGIN
    INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, estimated_cost,
                              status, pickup_location, dropoff_location, cancellation_reason,
                              created_at, updated_at)
    VALUES (2, 9, '2026-11-17', '2026-11-19', 56000.00,
            'CONFIRMED', 'Colombo', 'Negombo', NULL,
            '2026-10-08 09:10:00', '2026-10-08 09:10:00');
END@@
-- A second confirmed-unpaid journey is reserved for a simulated payment smoke
-- check. Keep the first one unspent for a manual customer test.
IF NOT EXISTS (
    SELECT 1 FROM reservations
    WHERE customer_id = 2 AND vehicle_id = 8
      AND start_date = '2026-11-27' AND end_date = '2026-11-29'
      AND status = 'CONFIRMED'
)
BEGIN
    INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, estimated_cost,
                              status, pickup_location, dropoff_location, cancellation_reason,
                              created_at, updated_at)
    VALUES (2, 8, '2026-11-27', '2026-11-29', 22000.00,
            'CONFIRMED', 'Kandy', 'Colombo', NULL,
            '2026-10-08 09:20:00', '2026-10-08 09:20:00');
END@@
-- Repair a prior local smoke-test row whose Corolla ID is absent from this DB.
IF NOT EXISTS (SELECT 1 FROM vehicle WHERE id = 1)
BEGIN
    UPDATE reservations SET vehicle_id = 8, estimated_cost = 22000.00
    WHERE customer_id = 2 AND vehicle_id = 1
      AND start_date = '2026-11-27' AND end_date = '2026-11-29'
      AND status = 'CONFIRMED';
END@@
-- Completed and already paid review-flow example with its invoice linked to the
-- exact reservation and existing vehicle rate.
IF NOT EXISTS (
    SELECT 1 FROM reservations
    WHERE customer_id = 2 AND vehicle_id = 3
      AND start_date = '2026-08-02' AND end_date = '2026-08-04'
      AND status = 'COMPLETED'
)
BEGIN
    INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, estimated_cost,
                              status, pickup_location, dropoff_location, cancellation_reason,
                              created_at, updated_at)
    VALUES (2, 3, '2026-08-02', '2026-08-04', 12000.00,
            'COMPLETED', 'Negombo', 'Colombo', NULL,
            '2026-08-02 08:30:00', '2026-08-04 17:00:00');
END@@
-- -----------------------------------------------------------------------------
-- 3. PAYMENTS + INVOICES (Payment module)
-- -----------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM payments)
BEGIN
    SET IDENTITY_INSERT payments ON;
    INSERT INTO payments (id, reservation_id, customer_id, amount, method, status,
                          transaction_reference, paid_at, refunded_at, failure_reason) VALUES
    (1, 1, 2, 25500.00, 'CREDIT_CARD',  'SUCCESS',  'TXN-RE-0001', '2026-09-01 09:20:00', NULL, NULL),
    (2, 2, 2, 87500.00, 'MOCK_GATEWAY', 'SUCCESS',  'TXN-RE-0002', '2026-09-14 11:40:00', NULL, NULL),
    (3, 3, 2, 35000.00, 'DEBIT_CARD',   'REFUNDED', 'TXN-RE-0003', '2026-08-05 10:05:00', '2026-08-06 08:25:00', NULL);
    SET IDENTITY_INSERT payments OFF;
END@@
IF NOT EXISTS (SELECT 1 FROM invoices)
BEGIN
    SET IDENTITY_INSERT invoices ON;
    INSERT INTO invoices (id, invoice_number, payment_id, base_amount, fees_amount, discount_amount,
                          total_amount, issued_at) VALUES
    (1, 'INV-2026-0001', 1, 24000.00, 1500.00, 0.00,    25500.00, '2026-09-01 09:20:30'),
    (2, 'INV-2026-0002', 2, 90000.00, 2500.00, 5000.00, 87500.00, '2026-09-14 11:40:30');
    SET IDENTITY_INSERT invoices OFF;
END@@
IF NOT EXISTS (SELECT 1 FROM payments WHERE transaction_reference = 'TXN-RE-CONFIRMED-PAID')
BEGIN
    INSERT INTO payments (reservation_id, customer_id, amount, method, status,
                          transaction_reference, paid_at, refunded_at, failure_reason)
    SELECT r.id, r.customer_id, 56500.00, 'CREDIT_CARD', 'SUCCESS',
           'TXN-RE-CONFIRMED-PAID', '2026-10-08 09:15:00', NULL, NULL
    FROM reservations r
    WHERE r.customer_id = 2 AND r.vehicle_id = 9
      AND r.start_date = '2026-11-17' AND r.end_date = '2026-11-19'
      AND r.status = 'CONFIRMED';
END@@
IF NOT EXISTS (SELECT 1 FROM payments WHERE transaction_reference = 'TXN-RE-COMPLETED-PAID')
BEGIN
    INSERT INTO payments (reservation_id, customer_id, amount, method, status,
                          transaction_reference, paid_at, refunded_at, failure_reason)
    SELECT r.id, r.customer_id, 12500.00, 'DEBIT_CARD', 'SUCCESS',
           'TXN-RE-COMPLETED-PAID', '2026-08-04 17:05:00', NULL, NULL
    FROM reservations r
    WHERE r.customer_id = 2 AND r.vehicle_id = 3
      AND r.start_date = '2026-08-02' AND r.end_date = '2026-08-04'
      AND r.status = 'COMPLETED';
END@@
IF NOT EXISTS (SELECT 1 FROM invoices WHERE invoice_number = 'INV-2026-TEST-CONF-PAID')
BEGIN
    INSERT INTO invoices (invoice_number, payment_id, base_amount, fees_amount, discount_amount,
                          total_amount, issued_at)
    SELECT 'INV-2026-TEST-CONF-PAID', p.id, 56000.00, 500.00, 0.00,
           56500.00, '2026-10-08 09:15:30'
    FROM payments p
    WHERE p.transaction_reference = 'TXN-RE-CONFIRMED-PAID'
      AND p.status = 'SUCCESS';
END@@
IF NOT EXISTS (SELECT 1 FROM invoices WHERE invoice_number = 'INV-2026-TEST-COMPLETED-PAID')
BEGIN
    INSERT INTO invoices (invoice_number, payment_id, base_amount, fees_amount, discount_amount,
                          total_amount, issued_at)
    SELECT 'INV-2026-TEST-COMPLETED-PAID', p.id, 12000.00, 500.00, 0.00,
           12500.00, '2026-08-04 17:05:30'
    FROM payments p
    WHERE p.transaction_reference = 'TXN-RE-COMPLETED-PAID'
      AND p.status = 'SUCCESS';
END@@

-- Rental-operations demo fixture: confirmed + paid + already handed over.
-- Stable natural keys keep this safe to load on every application start. The
-- row remains an active rental after restart until staff completes its return.
IF NOT EXISTS (
    SELECT 1 FROM reservations
    WHERE customer_id = 2 AND vehicle_id = 8
      AND start_date = '2026-10-08' AND end_date = '2026-10-10'
)
BEGIN
    INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, estimated_cost,
                              status, pickup_location, dropoff_location, cancellation_reason,
                              created_at, updated_at)
    VALUES (2, 8, '2026-10-08', '2026-10-10', 22000.00,
            'CONFIRMED', 'Colombo', 'Kandy', NULL,
            '2026-10-08 08:00:00', '2026-10-08 08:00:00');
END@@

IF NOT EXISTS (SELECT 1 FROM payments WHERE transaction_reference = 'TXN-RE-ACTIVE-PAID')
BEGIN
    INSERT INTO payments (reservation_id, customer_id, amount, method, status,
                          transaction_reference, paid_at, refunded_at, failure_reason)
    SELECT r.id, r.customer_id, 22500.00, 'CREDIT_CARD', 'SUCCESS',
           'TXN-RE-ACTIVE-PAID', '2026-10-08 08:15:00', NULL, NULL
    FROM reservations r
    WHERE r.customer_id = 2 AND r.vehicle_id = 8
      AND r.start_date = '2026-10-08' AND r.end_date = '2026-10-10';
END@@

IF NOT EXISTS (SELECT 1 FROM invoices WHERE invoice_number = 'INV-2026-TEST-ACTIVE-PAID')
BEGIN
    INSERT INTO invoices (invoice_number, payment_id, base_amount, fees_amount, discount_amount,
                          total_amount, issued_at)
    SELECT 'INV-2026-TEST-ACTIVE-PAID', p.id, 22000.00, 500.00, 0.00,
           22500.00, '2026-10-08 08:15:30'
    FROM payments p WHERE p.transaction_reference = 'TXN-RE-ACTIVE-PAID'
      AND p.status = 'SUCCESS';
END@@
-- Confirmed + paid pickup-stage fixture for exercising a complete handover and
-- return in the demo environment. It remains separate from the Prius active-
-- rental fixture so staff can demonstrate both states together.
IF NOT EXISTS (
    SELECT 1 FROM reservations
    WHERE customer_id = 2 AND vehicle_id = 4
      AND start_date = '2026-10-08' AND end_date = '2026-10-10'
)
BEGIN
    INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, estimated_cost,
                              status, pickup_location, dropoff_location, cancellation_reason,
                              created_at, updated_at)
    VALUES (2, 4, '2026-10-08', '2026-10-10', 30000.00,
            'CONFIRMED', 'Kandy', 'Colombo', NULL,
            '2026-10-08 08:05:00', '2026-10-08 08:05:00');
END@@

IF NOT EXISTS (SELECT 1 FROM payments WHERE transaction_reference = 'TXN-RE-PICKUP-PAID')
BEGIN
    INSERT INTO payments (reservation_id, customer_id, amount, method, status,
                          transaction_reference, paid_at, refunded_at, failure_reason)
    SELECT r.id, r.customer_id, 30500.00, 'DEBIT_CARD', 'SUCCESS',
           'TXN-RE-PICKUP-PAID', '2026-10-08 08:20:00', NULL, NULL
    FROM reservations r
    WHERE r.customer_id = 2 AND r.vehicle_id = 4
      AND r.start_date = '2026-10-08' AND r.end_date = '2026-10-10';
END@@

IF NOT EXISTS (SELECT 1 FROM invoices WHERE invoice_number = 'INV-2026-TEST-PICKUP-PAID')
BEGIN
    INSERT INTO invoices (invoice_number, payment_id, base_amount, fees_amount, discount_amount,
                          total_amount, issued_at)
    SELECT 'INV-2026-TEST-PICKUP-PAID', p.id, 30000.00, 500.00, 0.00,
           30500.00, '2026-10-08 08:20:30'
    FROM payments p WHERE p.transaction_reference = 'TXN-RE-PICKUP-PAID'
      AND p.status = 'SUCCESS';
END@@

-- -----------------------------------------------------------------------------
-- 6. DISCOUNT CODES
-- -----------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM discount_codes)
BEGIN
    SET IDENTITY_INSERT discount_codes ON;
    INSERT INTO discount_codes (id, code, percentage, valid_from, valid_until, active) VALUES
    (1, 'WELCOME10', 10.00, '2026-01-01', '2026-12-31', 1),
    (2, 'AVURUDU15', 15.00, '2026-04-01', '2026-04-30', 0),
    (3, 'LONGSTAY20', 20.00, '2026-09-01', '2026-12-31', 1);
    SET IDENTITY_INSERT discount_codes OFF;
END@@
-- -----------------------------------------------------------------------------
-- 4. CHECKOUT / RETURN RECORDS (Checkout module) — for the completed booking
-- -----------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM checkout_records)
BEGIN
    SET IDENTITY_INSERT checkout_records ON;
    INSERT INTO checkout_records (id, reservation_id, vehicle_id, stage, handover_odometer,
                                  handover_fuel_level, handover_timestamp, handover_photo_url, handover_staff_id,
                                  return_odometer, return_fuel_level, return_timestamp, return_photo_url, return_staff_id,
                                  expected_return_timestamp, late_fee) VALUES
    (1, 1, 1, 'RETURNED', 41500, 'FULL', '2026-09-05 09:30:00', NULL, 'STAFF-01',
         41890, 'HALF', '2026-09-08 16:50:00', NULL, 'STAFF-02',
         '2026-09-08 17:00:00', 0.00);
    SET IDENTITY_INSERT checkout_records OFF;
END@@
-- -----------------------------------------------------------------------------
-- 4b. DAMAGE REPORTS
-- -----------------------------------------------------------------------------
IF NOT EXISTS (
    SELECT 1 FROM checkout_records c JOIN reservations r ON r.id = c.reservation_id
    WHERE r.customer_id = 2 AND r.vehicle_id = 8
      AND r.start_date = '2026-10-08' AND r.end_date = '2026-10-10'
)
BEGIN
    INSERT INTO checkout_records (reservation_id, vehicle_id, stage, handover_odometer,
                                  handover_fuel_level, handover_timestamp, handover_photo_url, handover_staff_id,
                                  return_odometer, return_fuel_level, return_timestamp, return_photo_url, return_staff_id,
                                  expected_return_timestamp, late_fee)
    SELECT r.id, r.vehicle_id, 'HANDOVER', 28640, 'FULL', '2026-10-08 09:30:00', NULL, 'STAFF-1',
           NULL, NULL, NULL, NULL, NULL, '2026-10-10 17:00:00', 0.00
    FROM reservations r
    WHERE r.customer_id = 2 AND r.vehicle_id = 8
      AND r.start_date = '2026-10-08' AND r.end_date = '2026-10-10'
      AND r.status = 'CONFIRMED';
END@@

UPDATE vehicle SET status = 'BOOKED'
WHERE id = 8 AND EXISTS (
    SELECT 1 FROM checkout_records c JOIN reservations r ON r.id = c.reservation_id
    WHERE c.stage = 'HANDOVER' AND r.vehicle_id = vehicle.id
      AND r.customer_id = 2 AND r.start_date = '2026-10-08' AND r.end_date = '2026-10-10'
);@@
IF NOT EXISTS (SELECT 1 FROM damage_reports)
BEGIN
    SET IDENTITY_INSERT damage_reports ON;
    INSERT INTO damage_reports (id, checkout_record_id, description, severity, estimated_repair_cost,
                                photo_url, reported_at) VALUES
    (1, 1, 'Small scratch on the rear bumper, noticed at return.', 'MINOR', 6500.00, NULL, '2026-09-08 16:55:00');
    SET IDENTITY_INSERT damage_reports OFF;
END@@
-- -----------------------------------------------------------------------------
-- 5. REVIEWS (Reports & Reviews module)
-- -----------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM reviews)
BEGIN
    SET IDENTITY_INSERT reviews ON;
    INSERT INTO reviews (id, customer_id, vehicle_id, reservation_id, rating, comment, submitted_at, flagged) VALUES
    (1, 2, 1, 1, 5, 'Smooth ride, car was spotless and pickup was quick. Would rent again.', '2026-09-09 10:00:00', 0),
    (2, 2, 5, 3, 4, 'Lovely car for the wedding, driver-side mirror was a bit loose though.', '2026-08-12 14:20:00', 0),
    (3, 2, 10, NULL, 1, 'Terrible service, never again!!! (test entry for the flag workflow)', '2026-08-01 09:00:00', 1);
    SET IDENTITY_INSERT reviews OFF;
END@@
-- -----------------------------------------------------------------------------
-- 5b. FAVORITES (Booking module)
-- -----------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM favorites)
BEGIN
    SET IDENTITY_INSERT favorites ON;
    INSERT INTO favorites (id, customer_id, vehicle_id, saved_at) VALUES
    (1, 2, 9, '2026-09-15 19:30:00'),
    (2, 2, 2, '2026-09-16 08:10:00');
    SET IDENTITY_INSERT favorites OFF;
END@@
