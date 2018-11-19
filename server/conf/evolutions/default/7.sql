
# --- !Ups

ALTER TABLE blocks
ALTER COLUMN nonce TYPE BIGINT;

# --- !Downs

ALTER TABLE blocks
ALTER COLUMN nonce TYPE INT;
