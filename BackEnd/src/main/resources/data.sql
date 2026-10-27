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
-- เคลียร์ข้อมูลเดิมและอัปเดตข้อมูลสถานที่ท่องเที่ยวชุดใหม่
DELETE FROM app.attraction_catalog;

INSERT INTO app.attraction_catalog (partner_id, public_name, description, location, category, image_url, accessibility_information, is_pass_eligible, is_published, display_order, created_at, updated_at) 
VALUES
('partner-wr', 'Wat Rong Khun', 'Restored by National Artist Chalermchai Kositpipat, this former small temple was transformed into a stunning white masterpiece, drawing tourists year-round.', 'Mueang Chiang Rai', 'Temple', '/images/wat-rong-khun.jpg', 'Wheelchair accessible', true, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('partner-bd', 'Baan Dam Museum', 'Created by a local artist, this complex features various structures inspired by Lan Xang, Lanna, and Suvarnabhumi architecture, displaying unique concept collections like silverwork in the Lao House.', 'Nang Lae', 'Museum', '/images/baan-dam.jpg', 'Partial wheelchair access', true, true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('partner-sp', 'Singha Park', 'Thailand''s largest agro-tourism destination covering over 8,000 rai. It offers scenic hills, farm tours, African wildlife, cycling, adventure activities, and sports events.', 'Mae Kon', 'Nature', '/images/singha-park.jpg', 'Wheelchair accessible', true, true, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('partner-gt', 'Golden Triangle', 'The border region of Thailand, Laos, and Myanmar where the Ruak and Mekong rivers meet. Spanning about 950,000 km², it was named by the CIA and serves as a major trade hub.', 'Chiang Saen', 'Landmark', '/images/golden-triangle.jpg', 'Wheelchair accessible', true, true, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('partner-cf', 'Choui Fong Tea Plantation', 'Located in Mae Chan, this scenic 1.6 km² hillside plantation features a restaurant, bakery, and beverage service.', 'Mae Chan', 'Nature', '/images/choui-fong.jpg', 'Wheelchair accessible', true, true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);