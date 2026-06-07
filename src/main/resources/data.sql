-- ==========================================
-- 1. ROOM INVENTORY (Split Statuses)
-- ==========================================

-- Ready for new arrivals (Empty and Cleaned)
INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('101', 'Standard', 'VACANT', 'CLEAN');

INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('301', 'Suite', 'VACANT', 'CLEAN');

-- Checked out today, waiting for housekeeping (Empty but Dirty)
INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('102', 'Standard', 'VACANT', 'DIRTY');

-- Guest is staying over, housekeeping has already cleaned the room today
INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('201', 'Deluxe', 'OCCUPIED', 'CLEAN');

-- Guest is staying over, housekeeping has NOT cleaned the room yet today
INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('202', 'Deluxe', 'OCCUPIED', 'DIRTY');

-- Maintenance issue (Removed from inventory and marked for repair)
INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('103', 'Standard', 'OUT_OF_ORDER', 'MAINTENANCE');

-- Additional Standard Rooms
INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('104', 'Standard', 'VACANT', 'CLEAN');

INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('105', 'Standard', 'VACANT', 'CLEAN');

INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('106', 'Standard', 'VACANT', 'CLEAN');

-- Additional Deluxe Rooms (Crucial: 201 and 202 are occupied, so we need clean ones!)
INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('203', 'Deluxe', 'VACANT', 'CLEAN');

INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('204', 'Deluxe', 'VACANT', 'CLEAN');

INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('205', 'Deluxe', 'VACANT', 'CLEAN');

-- Additional Suites
INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('302', 'Suite', 'VACANT', 'CLEAN');

INSERT INTO room (room_number, type, occupancy_status, housekeeping_status) 
VALUES ('303', 'Suite', 'VACANT', 'CLEAN');


-- ==========================================
-- 2. RESERVATIONS (Dynamic Dates)
-- ==========================================

-- A. ARRIVING TODAY (Can be assigned to 101 or 301)
INSERT INTO reservation (
    reservation_code, guest_name, email, phone_number, room_type, 
    check_in_date, check_out_date, status, calculated_price, 
    guest_preferences, pre_ordered_amenities
) VALUES (
    'RES-100001', 'Jan Novak', 'jan@example.cz', '+420123456789', 'Suite', 
    CURRENT_DATE, DATEADD('DAY', 3, CURRENT_DATE), 'CONFIRMED', 1350.00,
    'Guest is highly allergic to down pillows. Flight arrives at 14:00.', 'Arrival Transport, Champagne'
);

-- B. CURRENTLY IN-HOUSE (Occupying 201 or 202)
INSERT INTO reservation (
    reservation_code, guest_name, email, phone_number, room_type, 
    check_in_date, check_out_date, status, calculated_price, 
    guest_preferences, pre_ordered_amenities
) VALUES (
    'RES-100002', 'Eva Svobodová', 'eva@example.cz', '+420987654321', 'Deluxe', 
    DATEADD('DAY', -2, CURRENT_DATE), DATEADD('DAY', 1, CURRENT_DATE), 'CHECKED_IN', 660.00,
    'Needs a quiet room away from the elevator.', 'None'
);

-- C. CHECKED OUT TODAY (Freed up room 102)
INSERT INTO reservation (
    reservation_code, guest_name, email, phone_number, room_type, 
    check_in_date, check_out_date, status, calculated_price, 
    guest_preferences, pre_ordered_amenities
) VALUES (
    'RES-100003', 'Petr Dvořák', 'petr@example.cz', '+420111222333', 'Standard', 
    DATEADD('DAY', -3, CURRENT_DATE), CURRENT_DATE, 'CHECKED_OUT', 330.00,
    'Late checkout requested (approved for 13:00).', 'None'
);

-- D. FUTURE BOOKING 
INSERT INTO reservation (
    reservation_code, guest_name, email, phone_number, room_type, 
    check_in_date, check_out_date, status, calculated_price, 
    guest_preferences, pre_ordered_amenities
) VALUES (
    'RES-100004', 'Lucie Černá', 'lucie@example.cz', '+420444555666', 'Standard', 
    DATEADD('DAY', 14, CURRENT_DATE), DATEADD('DAY', 20, CURRENT_DATE), 'CONFIRMED', 660.00,
    'Anniversary trip.', 'Flowers in room'
);


-- ==========================================
-- 3. MARKET RATES
-- ==========================================

INSERT INTO market_rate (room_type, average_price) VALUES ('Standard', 110.00);
INSERT INTO market_rate (room_type, average_price) VALUES ('Deluxe', 220.00);
INSERT INTO market_rate (room_type, average_price) VALUES ('Suite', 450.00);