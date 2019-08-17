
# --- !Ups

CREATE DOMAIN HASH_TYPE AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

ALTER TABLE blocks
ALTER COLUMN merkle_root TYPE HASH_TYPE USING DECODE(merkle_root, 'hex');

ALTER TABLE block_address_gcs
ALTER COLUMN hex TYPE BYTEA USING DECODE(hex, 'hex');

ALTER TABLE transaction_outputs
ALTER COLUMN hex_script TYPE BYTEA USING DECODE(hex_script, 'hex');

DROP DOMAIN HEX_STRING_TYPE;

# --- !Downs

CREATE DOMAIN HEX_STRING_TYPE AS TEXT
CHECK (
  VALUE ~ '^([a-f0-9][a-f0-9])+$'
);

ALTER TABLE transaction_outputs
ALTER COLUMN hex_script TYPE TEXT USING ENCODE(hex_script, 'hex');

ALTER TABLE block_address_gcs
ALTER COLUMN hex TYPE HEX_STRING_TYPE USING ENCODE(hex, 'hex')::HEX_STRING_TYPE;

ALTER TABLE blocks
ALTER COLUMN merkle_root TYPE TEXT USING ENCODE(merkle_root, 'hex');

DROP DOMAIN HASH_TYPE;
