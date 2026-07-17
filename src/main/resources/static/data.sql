-- Default product categories.
-- Uses INSERT IGNORE (MySQL) so this file can safely run on every
-- application startup (see spring.sql.init.mode=always) without creating
-- duplicates — `categories.name` has a UNIQUE constraint, so a repeat
-- insert is simply skipped.
--
-- Without this seed, a fresh database has zero categories, the shop's
-- "add product" category dropdown is empty, and every product submission
-- fails with "Category not found" (HTTP 404) because there is nothing to
-- select. Admins can still add more categories later from the admin
-- dashboard (POST /api/categories).

INSERT IGNORE INTO categories (name, description, created_at, updated_at) VALUES
('Vegetables', 'Fresh, locally grown vegetables', NOW(), NOW()),
('Fruits', 'Seasonal and exotic fruits', NOW(), NOW()),
('Dairy', 'Milk, curd, paneer, ghee and other dairy products', NOW(), NOW()),
('Grains & Pulses', 'Rice, wheat, lentils and other staples', NOW(), NOW()),
('Spices & Condiments', 'Whole and ground spices, masalas', NOW(), NOW()),
('Leafy Greens', 'Spinach, coriander, mint and other greens', NOW(), NOW()),
('Herbs', 'Fresh culinary herbs', NOW(), NOW()),
('Organic', 'Certified organic produce', NOW(), NOW()),
('Bakery', 'Bread, buns and other baked goods', NOW(), NOW()),
('Others', 'Anything that does not fit another category', NOW(), NOW());
