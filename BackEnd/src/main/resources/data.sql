-- reset data before run
DELETE FROM app.pass_catalog;
DELETE FROM app.attraction_catalog;

-- ==========================================
-- dummy data for table Pass Catalog
-- ==========================================
INSERT INTO app.pass_catalog (pass_id, display_name, description, price, currency, validity_description, is_available, is_published, display_order, created_at, updated_at) 
VALUES
('day-pass', 'Chiang Rai Day Pass', 'Visit participating attractions', 299.00, 'THB', 'Per day', true, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('explorer-pass', 'Explorer Pass', 'More time to explore popular spots', 599.00, 'THB', 'Per day', true, true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('family-pass', 'Family Pass', 'For up to 4 visitors', 999.00, 'THB', 'Per day', true, true, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
-- ==========================================
-- dummy data for table Attraction Catalog
-- ==========================================
INSERT INTO app.attraction_catalog (partner_id, public_name, description, location, category, image_url, accessibility_information, is_pass_eligible, is_published, display_order, created_at, updated_at) 
VALUES
('partner-wr', 'Wat Rong Khun', 'Discover the striking White Temple and its detailed contemporary artwork.', 'Mueang Chiang Rai', 'Temple', "D:\\Final_Project\\CEIpass\\BackEnd\\src\\main\\resources\\static\\images\\wat-rong-khun.jpg", 'Wheelchair accessible', true, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('partner-bd', 'Baan Dam Museum', 'Explore the distinctive art and architecture of the Black House Museum.', 'Nang Lae', 'Museum', "D:\\Final_Project\\CEIpass\\BackEnd\\src\\main\\resources\\static\\images\\baan-dam-museum.jpg", 'Partial wheelchair access', true, true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('partner-sp', 'Singha Park', 'Take in open landscapes, gardens, and outdoor activities in Chiang Rai.', 'Mae Kon', 'Nature', "D:\\Final_Project\\CEIpass\\BackEnd\\src\\main\\resources\\static\\images\\singha-park.jpg", 'Wheelchair accessible', true, true, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);