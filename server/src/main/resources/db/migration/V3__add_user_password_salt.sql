-- Temporary default of empty bytes allows adding a NOT NULL column to existing rows.
-- The default is dropped immediately after so new rows must always supply a salt.
ALTER TABLE "user"
    ADD COLUMN password_salt BYTEA NOT NULL DEFAULT '';

ALTER TABLE "user"
    ALTER COLUMN password_salt DROP DEFAULT;
