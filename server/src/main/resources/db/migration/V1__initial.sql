CREATE TABLE "user"
(
    id                UUID  NOT NULL PRIMARY KEY,
    username          TEXT  NOT NULL UNIQUE,
    password_md5_hash BYTEA NOT NULL,
    password_salt     BYTEA NOT NULL
);

CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE ingredient
(
    id                UUID   NOT NULL PRIMARY KEY,
    user_id           UUID   NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    name              CITEXT NOT NULL,
    UNIQUE (user_id, name)
);

CREATE TABLE chat
(
    id                UUID  NOT NULL PRIMARY KEY,
    user_id           UUID  NOT NULL REFERENCES "user" (id) ON DELETE CASCADE
);

CREATE INDEX "idx_user_id" ON chat(user_id);
