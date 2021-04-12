# --- !Ups

ALTER TABLE transactions ALTER COLUMN sent SET NOT NULL;
ALTER TABLE transactions ALTER COLUMN received SET NOT NULL;