-- Database: authdb

-- Set client encoding
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

--
-- Table structure for table "app_user"
--

DROP TABLE IF EXISTS app_user CASCADE;

CREATE TABLE app_user (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) DEFAULT NULL,
    last_name VARCHAR(255) DEFAULT NULL
);

--
-- Dumping data for table "app_user"
--

INSERT INTO app_user (id, email, password, first_name, last_name) VALUES
(1, 'panosdim@gmail.com', '$2a$10$14bdb30a9532c3ca582b3uc4wgQIWf3ol788aCWrb4ne8JlhpfvIG', 'Panagiotis', 'Dimopoulos');

-- Reset sequence for app_user table
SELECT setval('app_user_id_seq', (SELECT MAX(id) FROM app_user));
