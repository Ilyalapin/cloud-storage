CREATE TABLE users
(
    id       SERIAL PRIMARY KEY,
    login    VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100)        NOT NULL
);