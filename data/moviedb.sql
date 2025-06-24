-- Database: moviedb

-- Set client encoding
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

--
-- Table structure for table "movie"
--

DROP TABLE IF EXISTS movie CASCADE;

CREATE TABLE "movie" (
	"id" SERIAL PRIMARY KEY,
	"movie_id" INTEGER NULL DEFAULT NULL,
	"poster" VARCHAR(255) NULL DEFAULT NULL,
	"rating" INTEGER NULL DEFAULT NULL,
	"title" VARCHAR(255) NULL DEFAULT NULL,
	"user_id" VARCHAR(255) NULL DEFAULT NULL,
	"watched" BOOLEAN NULL DEFAULT NULL
);

-- Create index on user_id
CREATE INDEX movie_user_id ON movie(user_id);

--
-- Dumping data for table "movie"
--

INSERT INTO movie (user_id, title, poster, movie_id, watched, rating) VALUES
('panosdim@gmail.com', 'Deadpool & Wolverine', '/uxBHXaoOvAwy4NpPpP7nNx2rXYQ.jpg', 533535, TRUE, 3),
('panosdim@gmail.com', 'Despicable Me 4', '/aun2IpLDx2E08hYj8xnAEUEFLlo.jpg', 519182, TRUE, 4),
('panosdim@gmail.com', 'Gladiator II', '/f54mzACTFdiAxnQ30BK4GjrKzyn.jpg', 558449, TRUE, 4),
('panosdim@gmail.com', 'Captain America: Brave New World', '/jmobYdUXrbj1g5QFWjeomOk2Noz.jpg', 822119, TRUE, 0),
('panosdim@gmail.com', 'Alien: Romulus', '/b33nnKl1GSFbao4l3fZDDqsMx0F.jpg', 945961, TRUE, 3),
('panosdim@gmail.com', 'Inside Out 2', '/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg', 1022789, TRUE, 4),
('panosdim@gmail.com', 'Kung Fu Panda 4', '/kDp1vUBnMpe8ak4rjgl3cLELqjU.jpg', 1011985, TRUE, 4),
('panosdim@gmail.com', 'Kingdom of the Planet of the Apes', '/gKkl37BQuKTanygYQG1pyYgLVgf.jpg', 653346, TRUE, 4),
('panosdim@gmail.com', 'Bad Boys: Ride or Die', '/oGythE98MYleE6mZlGs5oBGkux1.jpg', 573435, TRUE, 3),
('panosdim@gmail.com', 'A Quiet Place: Day One', '/hU42CRk14JuPEdqZG3AWmagiPAP.jpg', 762441, TRUE, 2),
('panosdim@gmail.com', '28 Years Later', '/5SyBozBOc2PGH6QIIlMDpfOCB5b.jpg', 1100988, FALSE, 0),
('panosdim@gmail.com', 'The Wild Robot', '/wTnV3PCVW5O92JMrFvvrRcV39RU.jpg', 1184918, TRUE, 5),
('panosdim@gmail.com', 'Venom: The Last Dance', '/8mRrl8lc7TrbdA1PFzUhQ0nFZ7R.jpg', 912649, TRUE, 3),
('panosdim@gmail.com', 'Red One', '/cdqLnri3NEGcmfnqwk2TSIYtddg.jpg', 845781, TRUE, 3),
('panosdim@gmail.com', 'Avengers: Infinity War', '/7WsyChQLEftFiDOVTGkv3hFpyyt.jpg', 299536, TRUE, 5),
('panosdim@gmail.com', 'Iron Man', '/78lPtwv72eTNqFW9COBYI0dWDJa.jpg', 1726, TRUE, 5),
('panosdim@gmail.com', 'Se7en', '/191nKfP0ehp3uIvWqgPbFmI4lv9.jpg', 807, TRUE, 5),
('panosdim@gmail.com', 'Wolfs', '/vOX1Zng472PC2KnS0B9nRfM8aaZ.jpg', 877817, TRUE, 3),
('panosdim@gmail.com', 'Avengers: Endgame', '/or06FN3Dka5tukK1e9sl16pB3iy.jpg', 299534, TRUE, 5),
('panosdim@gmail.com', 'Canary Black', '/hhiR6uUbTYYvKoACkdAIQPS5c6f.jpg', 976734, TRUE, 3),
('panosdim@gmail.com', 'Mission: Impossible - The Final Reckoning', '/z53D72EAOxGRqdr7KXXWp9dJiDe.jpg', 575265, FALSE, 0),
('panosdim@gmail.com', 'Moana 2', '/yh64qw9mgXBvlaWDi7Q9tpUBAvH.jpg', 1241982, TRUE, 4),
('panosdim@gmail.com', 'Kraven the Hunter', '/i47IUSsN126K11JUzqQIOi1Mg1M.jpg', 539972, TRUE, 3),
('panosdim@gmail.com', 'The Beekeeper', '/A7EByudX0eOzlkQ2FIbogzyazm2.jpg', 866398, TRUE, 3),
('panosdim@gmail.com', 'Black Bag', '/yCVJ8joYTzf9tCby9o9H7zhEVG8.jpg', 1233575, TRUE, 0),
('panosdim@gmail.com', 'Jurassic World Rebirth', '/8BZ6oGDE336qFshFC44WxZtosBX.jpg', 1234821, FALSE, 0),
('panosdim@gmail.com', 'Thunderbolts*', '/jrlCcyFJek3BWnZhpU6MHtTMoDK.jpg', 986056, FALSE, 0),
('panosdim@gmail.com', 'Mickey 17', '/lrCcovGRcuv8Z1v3ae1ZH5Ird05.jpg', 696506, TRUE, 0),
('panosdim@gmail.com', 'The Gorge', '/7iMBZzVZtG0oBug4TfqDb9ZxAOa.jpg', 950396, TRUE, 4),
('panosdim@gmail.com', 'Southpaw', '/kSQ49Fi3NVTqGGXILmxV2T2pdkG.jpg', 307081, TRUE, 4),
('panosdim@gmail.com', 'Sinners', '/yqsCU5XOP2mkbFamzAqbqntmfav.jpg', 1233413, TRUE, 0),
('panosdim@gmail.com', 'The Accountant 2', '/4rxr2grcBfxwH4penSxndcPwyDp.jpg', 870028, TRUE, 0);

-- Reset sequence for movie table
SELECT setval('movie_id_seq', (SELECT MAX(id) FROM movie));