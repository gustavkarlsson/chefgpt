CREATE TABLE "user"
(
    id                UUID  NOT NULL PRIMARY KEY,
    username          TEXT  NOT NULL,
    password_md5_hash BYTEA NOT NULL
);
