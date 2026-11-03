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
-- Dummy Data for Table Attraction Catalog (Extended Descriptions)
-- ==========================================
DELETE FROM app.attraction_catalog;

INSERT INTO app.attraction_catalog (partner_id, public_name, description, location, category, image_url, accessibility_information, is_pass_eligible, is_published, display_order, created_at, updated_at)
VALUES
('TEMPLE-01', 'Wat Rong Khun (White Temple)', 'Restored and envisioned by National Artist Chalermchai Kositpipat, this former small temple was transformed into a stunning white masterpiece. The design incorporates intricate mirrors reflecting light, contemporary art, and symbolic Buddhist iconography representing the journey from the hell of human desire to ultimate enlightenment.', 'Chiang Rai, Thailand', 'TEMPLE', '/images/wat-rong-khun.jpg', 'Parking available; wheelchair access should be confirmed', true, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('TEMPLE-02', 'Wat Rong Suea Ten (Blue Temple)', 'A striking contemporary temple covered in deep sapphire blue and golden yellow highlights, created by Phuttha Kabkaew (a student of Chalermchai Kositpipat). Famous for its vivid modern murals covering the walls and a large, serene white Buddha image seated at the head of the hall.', 'Chiang Rai, Thailand', 'TEMPLE', '/images/wat-rong-suea-ten.jpg', 'Parking and access facilities should be confirmed', true, true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('TEMPLE-03', 'Wat Huay Pla Kang', 'A famous hilltop temple complex known for its extraordinary nine-tiered pagoda (Phra Maha Chedi) combining Lanna and Chinese architecture, along with a colossal white statue of Guanyin (Goddess of Mercy) overlooking the valley. Visitors can take an elevator inside the statue to enjoy panoramic views.', 'Chiang Rai, Thailand', 'TEMPLE', '/images/wat-huay-pla-kang.jpg', 'Hilltop location; wheelchair access should be confirmed', true, true, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('TEMPLE-04', 'Wat Phra Singh Chiang Rai', 'A revered historic temple situated in the heart of Chiang Rai city centre. Dating back to the 14th century, it once housed the sacred Phra Singh Buddha image and features classical Lanna-style wooden structures, intricate carvings, and a beautiful golden chedi.', 'Chiang Rai, Thailand', 'TEMPLE', '/images/wat-phra-singh.jpg', 'Accessibility facilities should be confirmed', true, true, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('TEMPLE-05', 'Wat Phra Kaew Chiang Rai', 'One of northern Thailand''s most historic temples, famous as the site where the sacred Emerald Buddha was first discovered in 1434 after lightning struck its chedi. The complex also houses an exquisite museum showcasing valuable Lanna religious art, antiques, and a new Emerald Buddha replica.', 'Chiang Rai, Thailand', 'TEMPLE', '/images/wat-phra-kaew.jpg', 'Accessibility facilities should be confirmed', true, true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('MUSEUM-01', 'Baan Dam Museum', 'Created by national artist Thawan Duchanee, this distinctive complex consists of over 40 dark wooden structures inspired by Lan Xang, Lanna, and Suvarnabhumi architecture. It serves as an art space displaying unique collections of animal bones, horns, skins, and intricate silverwork in the Lao House.', 'Chiang Rai, Thailand', 'MUSEUM', '/images/baan-dam.jpg', 'Walking paths; wheelchair access should be confirmed', true, true, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('MUSEUM-02', 'Oub Kham Museum', 'A private heritage museum exhibiting rare and irreplaceable Lanna artefacts, royal regalia, ancient costumes, and tribal jewellery collected from northern Thailand, Myanmar, Laos, and southwestern China, preserving the rich cultural identity of the golden Golden Triangle region.', 'Chiang Rai, Thailand', 'MUSEUM', '/images/oub-kham-museum.jpg', 'Accessibility facilities should be confirmed', true, true, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('MUSEUM-03', 'Hill Tribe Museum', 'An educational museum run by the Population and Community Development Association (PDA). It introduces visitors to the history, traditional attire, crafts, slash-and-burn agricultural practices, and daily life of the major hill tribe communities across northern Thailand.', 'Chiang Rai, Thailand', 'MUSEUM', '/images/hill-tribe-museum.jpg', 'Audio-visual exhibits and multilingual learning resources', true, true, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('NATURE-01', 'Singha Park Chiang Rai', 'Thailand''s largest agro-tourism destination spanning over 8,000 rai (approx 3.2 sq km). Features scenic rolling hills, tea plantation tours, fruit orchards, African wildlife areas (giraffes & zebras), cycling trails, adventure ziplining, and seasonal hot air balloon festivals.', 'Chiang Rai, Thailand', 'NATURE', '/images/singha-park.jpg', 'Parking and sightseeing tram available; accessibility details should be confirmed', true, true, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('CULTURE-01', 'Mae Fah Luang Art and Cultural Park', 'A peaceful botanical garden and cultural centre featuring traditional teakwood Lanna structures, including Haw Kham (The Golden Pavilion). It displays historical wooden religious craftsmanship, antique lanterns, and Lanna art surrounding a serene lotus pond.', 'Chiang Rai, Thailand', 'CULTURAL', '/images/mae-fah-luang-art-park.jpg', 'Garden paths and exhibition access to be confirmed', true, true, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('MUSEUM-05', 'Chiang Saen National Museum', 'A national museum located near the ancient city walls of Chiang Saen. It preserves and displays precious archaeological finds, stone inscriptions, Lanna-style bronze Buddha images, and historical relics excavated from ancient temples along the Mekong River basin.', 'Chiang Saen, Chiang Rai, Thailand', 'MUSEUM', '/images/chiang-saen-museum.jpg', 'Entrance ramp reported; upper-floor access may be limited by stairs. Confirm facilities with the museum.', true, true, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('CULTURE-02', 'Hall of Opium', 'A world-class interactive exhibition museum located near the Golden Triangle. It presents the comprehensive 5,000-year history of opium—from its medicinal origins and trade wars to its social impacts and regional rehabilitation projects initiated by the Royal Project.', 'Chiang Saen, Chiang Rai, Thailand', 'MUSEUM', '/images/hall-of-opium.jpg', 'Indoor exhibition; accessibility details to be confirmed', true, true, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('NATURE-02', 'Choui Fong Tea Plantation', 'Located in Mae Chan district, this famous 1.6 sq km hillside tea plantation features neatly manicured green tea terraces cascading down the mountainside. Visitors can observe tea leaf picking and relax at modern architectural cafes serving fresh tea, cakes, and light meals.', 'Mae Chan, Chiang Rai, Thailand', 'NATURE', '/images/choui-fong.jpg', 'Sloping tea plantation terrain; access details to be confirmed', true, true, 13, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('NATURE-03', 'Mae Fah Luang Garden, Doi Tung', 'A beautifully manicured high-altitude botanical garden located on Doi Tung mountain. Built to honor HRH Princess Srinagarindra, it displays exotic winter flowers, native northern orchids, fountains, and sculptures amid cool mountain air year-round.', 'Mae Fa Luang, Chiang Rai, Thailand', 'NATURE', '/images/mae-fah-luang-garden.jpg', 'Outdoor garden paths; terrain varies', true, true, 14, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('NATURE-04', 'Tham Luang–Khun Nam Nang Non National Park', 'A famous national park encompassing forested mountains, natural emerald lagoons, and the cave complex of Tham Luang. Known globally for the international 2018 cave rescue operation, featuring an exhibition center and memorial dedicated to the rescue efforts.', 'Mae Sai, Chiang Rai, Thailand', 'NATURE', '/images/tham-luang.jpg', 'Cave and park access depends on designated routes and conditions', true, true, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('ART-01', 'Art Bridge Chiang Rai (Khua Silpa)', 'A vibrant riverside art space established by local Chiang Rai artists to support and cultivate northern Thai contemporary art. The complex houses rotation galleries, souvenir shops selling original art prints, workshop areas, and a picturesque riverside cafe.', 'Rim Kok, Mueang Chiang Rai, Thailand', 'ART_GALLERY', '/images/art-bridge.jpg', 'Accessibility facilities to be confirmed', true, true, 16, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('ART-02', 'Chiangrai Contemporary Art Museum', 'A dedicated modern gallery space promoting contemporary fine arts. It features rotated exhibitions from emerging local Chiang Rai creators as well as acclaimed international artists, serving as a hub for artistic inspiration and creative dialogue in northern Thailand.', 'Rim Kok, Mueang Chiang Rai, Thailand', 'ART_GALLERY', '/images/chiangrai-contemporary-art.jpg', NULL, true, true, 17, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('partner-gt', 'Golden Triangle', 'The famous geographical border confluence where Thailand, Laos, and Myanmar meet along the Mekong and Ruak Rivers. Historically coined by the CIA, it serves as a major regional trade crossroads, offering river boat tours and views of three countries at once.', 'Chiang Saen, Chiang Rai, Thailand', 'LANDMARK', '/images/golden-triangle.jpg', 'Wheelchair accessible', false, true, 18, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
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
