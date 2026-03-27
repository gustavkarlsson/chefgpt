CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE ingredient
(
    id       UUID   NOT NULL PRIMARY KEY,
    user_id  UUID   NOT NULL REFERENCES "user" (id),
    name     CITEXT NOT NULL,
    UNIQUE (user_id, name)
);
