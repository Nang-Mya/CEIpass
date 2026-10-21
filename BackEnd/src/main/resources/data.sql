-- reset data before run
DELETE FROM app.pass_catalog;
DELETE FROM app.attraction_catalog;

-- ==========================================
-- dummy data for table Pass Catalog
-- ==========================================
INSERT INTO app.pass_catalog (pass_id, display_name, description, price, currency, validity_description, is_available, is_published, display_order, duration_days, created_at, updated_at) 
VALUES
('day-pass', 'Chiang Rai Day Pass', '1 day · Visit participating attractions', 299.00, 'THB', '1 day', true, true, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('three-day-pass', 'Explorer Pass', '3 days · More time to explore', 599.00, 'THB', '3 days', true, true, 2, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('family-pass', 'Family Pass', '1 day · For up to 4 visitors', 999.00, 'THB', '1 day', true, true, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==========================================
-- dummy data for table Attraction Catalog
-- ==========================================
INSERT INTO app.attraction_catalog (partner_id, public_name, description, location, category, image_url, accessibility_information, is_pass_eligible, is_published, display_order, created_at, updated_at) 
VALUES
('partner-wr', 'Wat Rong Khun', 'Discover the striking White Temple and its detailed contemporary artwork.', 'Mueang Chiang Rai', 'Temple', NULL, 'Wheelchair accessible', true, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('partner-bd', 'Baan Dam Museum', 'Explore the distinctive art and architecture of the Black House Museum.', 'Nang Lae', 'Museum', NULL, 'Partial wheelchair access', true, true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('partner-sp', 'Singha Park', 'Take in open landscapes, gardens, and outdoor activities in Chiang Rai.', 'Mae Kon', 'Nature', NULL, 'Wheelchair accessible', true, true, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);