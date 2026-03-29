CREATE TABLE chat
(
    id                UUID  NOT NULL PRIMARY KEY,
    user_id           UUID  NOT NULL REFERENCES "user" (id)
);

CREATE INDEX "idx_user_id" ON chat(user_id);
