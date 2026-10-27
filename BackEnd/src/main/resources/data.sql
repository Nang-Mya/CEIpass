-- reset data before run (children first, then parents)
DELETE FROM app.benefit_usage_log;
DELETE FROM app.benefit;
DELETE FROM app.pass_catalog;
DELETE FROM app.attraction_catalog;

-- ==========================================
-- dummy data for table Pass Catalog
-- (pass_id kept as day-pass / explorer-pass / family-pass because
--  DemoDataInitializer creates the demo user pass with 'day-pass')
-- ==========================================
INSERT INTO app.pass_catalog (pass_id, display_name, description, price, currency, validity_description, is_available, is_published, display_order, created_at, updated_at)
VALUES
('day-pass', 'Chiang Rai Day Pass', 'Visit participating attractions', 299.00, 'THB', 'Per day', true, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('explorer-pass', 'Explorer Pass', 'More time to explore popular spots', 599.00, 'THB', '3 days', true, true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('family-pass', 'Family Pass', 'For up to 4 visitors', 999.00, 'THB', 'Per day', true, true, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==========================================
-- dummy data for table Attraction Catalog
-- ==========================================
INSERT INTO app.attraction_catalog (partner_id, public_name, description, location, category, image_url, accessibility_information, is_pass_eligible, is_published, display_order, created_at, updated_at)
VALUES
('TEMPLE-01', 'Wat Rong Khun (White Temple)', 'Restored by National Artist Chalermchai Kositpipat, this former small temple was transformed into a stunning white masterpiece, drawing tourists year-round.', 'Chiang Rai, Thailand', 'TEMPLE', '/images/wat-rong-khun.jpg', 'Parking available; wheelchair access should be confirmed', true, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TEMPLE-02', 'Wat Rong Suea Ten (Blue Temple)', 'A striking temple covered in deep blue and gold, known for its vivid murals and large white Buddha image.', 'Chiang Rai, Thailand', 'TEMPLE', '/images/wat-rong-suea-ten.jpg', 'Parking and access facilities should be confirmed', true, true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TEMPLE-03', 'Wat Huay Pla Kang', 'A hilltop temple with a nine-tiered pagoda and a giant white Guanyin statue overlooking the valley.', 'Chiang Rai, Thailand', 'TEMPLE', '/images/wat-huay-pla-kang.jpg', 'Hilltop location; wheelchair access should be confirmed', true, true, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TEMPLE-04', 'Wat Phra Singh Chiang Rai', 'A historic temple in the city centre housing a revered Buddha image in classic Lanna style.', 'Chiang Rai, Thailand', 'TEMPLE', '/images/wat-phra-singh.jpg', 'Accessibility facilities should be confirmed', true, true, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TEMPLE-05', 'Wat Phra Kaew Chiang Rai', 'The temple where the Emerald Buddha was first discovered, with a museum of Lanna art and history.', 'Chiang Rai, Thailand', 'TEMPLE', '/images/wat-phra-kaew.jpg', 'Accessibility facilities should be confirmed', true, true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MUSEUM-01', 'Baan Dam Museum', 'Created by a local artist, this complex features various structures inspired by Lan Xang, Lanna, and Suvarnabhumi architecture, displaying unique concept collections like silverwork in the Lao House.', 'Chiang Rai, Thailand', 'MUSEUM', '/images/baan-dam.jpg', 'Walking paths; wheelchair access should be confirmed', true, true, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MUSEUM-02', 'Oub Kham Museum', 'A private museum displaying rare Lanna artefacts, textiles and royal regalia from northern Thailand and neighbouring regions.', 'Chiang Rai, Thailand', 'MUSEUM', '/images/oub-kham-museum.jpg', 'Accessibility facilities should be confirmed', true, true, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MUSEUM-03', 'Hill Tribe Museum', 'A small museum introducing the history, crafts and daily life of the hill tribe communities of northern Thailand.', 'Chiang Rai, Thailand', 'MUSEUM', '/images/hill-tribe-museum.jpg', 'Audio-visual exhibits and multilingual learning resources', true, true, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NATURE-01', 'Singha Park Chiang Rai', 'Thailand''s largest agro-tourism destination covering over 8,000 rai. It offers scenic hills, farm tours, African wildlife, cycling, adventure activities, and sports events.', 'Chiang Rai, Thailand', 'NATURE', '/images/singha-park.jpg', 'Parking and sightseeing tram available; accessibility details should be confirmed', true, true, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CULTURE-01', 'Mae Fah Luang Art and Cultural Park', 'A park and museum showcasing Lanna art, wooden architecture and cultural collections in a peaceful garden setting.', 'Chiang Rai, Thailand', 'CULTURAL', '/images/mae-fah-luang-art-park.jpg', 'Garden paths and exhibition access to be confirmed', true, true, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MUSEUM-05', 'Chiang Saen National Museum', 'A national museum presenting archaeological finds and the history of the ancient city of Chiang Saen.', 'Chiang Saen, Chiang Rai, Thailand', 'MUSEUM', '/images/chiang-saen-museum.jpg', 'Entrance ramp reported; upper-floor access may be limited by stairs. Confirm facilities with the museum.', true, true, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CULTURE-02', 'Hall of Opium', 'A large exhibition hall near the Golden Triangle telling the history of opium in the region.', 'Chiang Saen, Chiang Rai, Thailand', 'MUSEUM', '/images/hall-of-opium.jpg', 'Indoor exhibition; accessibility details to be confirmed', true, true, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NATURE-02', 'Choui Fong Tea Plantation', 'Located in Mae Chan, this scenic 1.6 km2 hillside plantation features a restaurant, bakery, and beverage service.', 'Mae Chan, Chiang Rai, Thailand', 'NATURE', '/images/choui-fong.jpg', 'Sloping tea plantation terrain; access details to be confirmed', true, true, 13, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NATURE-03', 'Mae Fah Luang Garden,Doi Tung', 'A landscaped hillside garden on Doi Tung with seasonal flowers and cool mountain air.', 'Mae Fa Luang, Chiang Rai, Thailand', 'NATURE', '/images/mae-fah-luang-garden.jpg', 'Outdoor garden paths; terrain varies', true, true, 14, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NATURE-04', 'Tham Luang–Khun Nam Nang Non National Park', 'A national park with forested mountains and the Tham Luang cave, open along designated routes.', 'Mae Sai, Chiang Rai, Thailand', 'NATURE', '/images/tham-luang.jpg', 'Cave and park access depends on designated routes and conditions', true, true, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ART-01', 'Art Bridge Chiang Rai (Khua Silpa)', 'A riverside art space where local artists display and sell their work, with a cafe.', 'Rim Kok, Mueang Chiang Rai, Thailand', 'ART_GALLERY', '/images/art-bridge.jpg', 'Accessibility facilities to be confirmed', true, true, 16, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ART-02', 'Chiangrai Contemporary Art Museum', 'A contemporary art museum showing works by Chiang Rai and international artists.', 'Rim Kok, Mueang Chiang Rai, Thailand', 'ART_GALLERY', '/images/chiangrai-contemporary-art.jpg', NULL, true, true, 17, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- kept from friend's data: not covered by any pass yet
('partner-gt', 'Golden Triangle', 'The border region of Thailand, Laos, and Myanmar where the Ruak and Mekong rivers meet, long known as a major trade hub.', 'Chiang Saen, Chiang Rai, Thailand', 'LANDMARK', '/images/golden-triangle.jpg', 'Wheelchair accessible', false, true, 18, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==========================================
-- dummy data for table Benefit
-- ==========================================
INSERT INTO app.benefit (benefit_id, partner_id, pass_id, benefit_detail, discount_rate, usage_rule_limit, created_at, updated_at)
VALUES
-- day-pass
('BEN-1DAY-TEMPLE-01', 'TEMPLE-01', 'day-pass', '30% off the admission ticket to Wat Rong Khun', 30.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-1DAY-TEMPLE-02', 'TEMPLE-02', 'day-pass', '10% off at the temple souvenir shop', 10.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-1DAY-TEMPLE-03', 'TEMPLE-03', 'day-pass', '10% off at the temple souvenir shop', 10.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-1DAY-TEMPLE-04', 'TEMPLE-04', 'day-pass', '10% off at the temple souvenir shop', 10.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-1DAY-TEMPLE-05', 'TEMPLE-05', 'day-pass', '10% off at the temple souvenir shop', 10.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-1DAY-MUSEUM-01', 'MUSEUM-01', 'day-pass', 'Free admission ticket to Baan Dam Museum', 100.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-1DAY-MUSEUM-02', 'MUSEUM-02', 'day-pass', '30% off the admission ticket to Oub Kham Museum', 30.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-1DAY-MUSEUM-03', 'MUSEUM-03', 'day-pass', 'Free admission ticket to Hill Tribe Museum', 100.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-1DAY-NATURE-01', 'NATURE-01', 'day-pass', '30% off the sightseeing tram ride at Singha Park.', 30.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-1DAY-CULTURE-01', 'CULTURE-01', 'day-pass', '30% off the admission ticket to Mae Fah Luang Art and Cultural Park', 30.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-1DAY-ART-01', 'ART-01', 'day-pass', '10% off artworks and drinks at Art Bridge', 10.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-1DAY-ART-02', 'ART-02', 'day-pass', '10% off at the museum shop', 10.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- explorer-pass
('BEN-3DAY-TEMPLE-01', 'TEMPLE-01', 'explorer-pass', '30% off the admission ticket to Wat Rong Khun', 30.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-TEMPLE-02', 'TEMPLE-02', 'explorer-pass', '10% off at the temple souvenir shop', 10.00, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-TEMPLE-03', 'TEMPLE-03', 'explorer-pass', '10% off at the temple souvenir shop', 10.00, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-TEMPLE-04', 'TEMPLE-04', 'explorer-pass', '10% off at the temple souvenir shop', 10.00, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-TEMPLE-05', 'TEMPLE-05', 'explorer-pass', '10% off at the temple souvenir shop', 10.00, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-MUSEUM-01', 'MUSEUM-01', 'explorer-pass', 'Free admission ticket to Baan Dam Museum', 100.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-MUSEUM-02', 'MUSEUM-02', 'explorer-pass', '30% off the admission ticket to Oub Kham Museum', 30.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-MUSEUM-03', 'MUSEUM-03', 'explorer-pass', 'Free admission ticket to Hill Tribe Museum', 100.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-NATURE-01', 'NATURE-01', 'explorer-pass', '30% off the sightseeing tram ride at Singha Park.', 30.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-CULTURE-01', 'CULTURE-01', 'explorer-pass', '30% off the admission ticket to Mae Fah Luang Art and Cultural Park', 30.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-MUSEUM-05', 'MUSEUM-05', 'explorer-pass', 'Free admission ticket to Chiang Saen National Museum', 100.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-CULTURE-02', 'CULTURE-02', 'explorer-pass', '30% off the admission ticket to Hall of Opium', 30.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-NATURE-02', 'NATURE-02', 'explorer-pass', '20% off tea and bakery at the plantation cafe', 20.00, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-NATURE-03', 'NATURE-03', 'explorer-pass', 'Free admission ticket to Mae Fah Luang Garden', 100.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-NATURE-04', 'NATURE-04', 'explorer-pass', 'Free park admission (designated routes only)', 100.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-ART-01', 'ART-01', 'explorer-pass', '10% off artworks and drinks at Art Bridge', 10.00, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-3DAY-ART-02', 'ART-02', 'explorer-pass', 'Free admission ticket to Chiangrai Contemporary Art Museum', 100.00, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- family-pass
('BEN-FAMILY-TEMPLE-01', 'TEMPLE-01', 'family-pass', '30% off the admission ticket to Wat Rong Khun (up to 4 family members)', 30.00, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-FAMILY-TEMPLE-02', 'TEMPLE-02', 'family-pass', '10% off at the temple souvenir shop', 10.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-FAMILY-TEMPLE-03', 'TEMPLE-03', 'family-pass', '10% off at the temple souvenir shop', 10.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-FAMILY-TEMPLE-04', 'TEMPLE-04', 'family-pass', '10% off at the temple souvenir shop', 10.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-FAMILY-TEMPLE-05', 'TEMPLE-05', 'family-pass', '10% off at the temple souvenir shop', 10.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-FAMILY-MUSEUM-01', 'MUSEUM-01', 'family-pass', 'Free admission ticket to Baan Dam Museum (up to 4 family members)', 100.00, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-FAMILY-MUSEUM-02', 'MUSEUM-02', 'family-pass', '30% off the admission ticket to Oub Kham Museum (up to 4 family members)', 30.00, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-FAMILY-MUSEUM-03', 'MUSEUM-03', 'family-pass', 'Free admission ticket to Hill Tribe Museum (up to 4 family members)', 100.00, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-FAMILY-NATURE-01', 'NATURE-01', 'family-pass', '30% off the sightseeing tram ride at Singha Park (up to 4 family members)', 30.00, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-FAMILY-CULTURE-01', 'CULTURE-01', 'family-pass', '30% off the admission ticket to Mae Fah Luang Art and Cultural Park (up to 4 family members)', 30.00, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-FAMILY-ART-01', 'ART-01', 'family-pass', '10% off artworks and drinks at Art Bridge', 10.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-FAMILY-ART-02', 'ART-02', 'family-pass', 'Free admission ticket (up to 4 family members)', 100.00, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
