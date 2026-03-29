CREATE TABLE event
(
    id                UUID NOT NULL PRIMARY KEY,
    chat_id           UUID NOT NULL REFERENCES chat (id) ON DELETE CASCADE,
    json              jsonb NOT NULL
);
