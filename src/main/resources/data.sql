-- Initial Rooms
INSERT INTO room (room_number, type, status) VALUES ('101', 'Deluxe', 'CLEAN_VACANT');
INSERT INTO room (room_number, type, status) VALUES ('102', 'Deluxe', 'CLEAN_VACANT');
INSERT INTO room (room_number, type, status) VALUES ('201', 'Suite', 'CLEAN_VACANT');

-- A test reservation ready for Check-in (Process 2)
INSERT INTO reservation (booking_reference, guest_name, email, phone_number, room_type, check_in_date, check_out_date, status) 
VALUES ('RES-123456', 'Jan Novak', 'jan@example.cz', '+420123456789', 'Deluxe', '2026-06-03', '2026-06-06', 'CONFIRMED');