
# --- !Ups

-- drop FK constraints to be able to update column types one after another
ALTER TABLE transactions DROP CONSTRAINT transactions_blockhash_fk;
ALTER TABLE block_address_gcs DROP CONSTRAINT block_address_gcs_blockhash_fk;

-- update main types
ALTER TABLE blocks
    ALTER COLUMN blockhash TYPE HASH_TYPE USING DECODE(blockhash, 'hex'),
    ALTER COLUMN previous_blockhash TYPE HASH_TYPE USING DECODE(previous_blockhash, 'hex'),
    ALTER COLUMN next_blockhash TYPE HASH_TYPE USING DECODE(next_blockhash, 'hex');

-- update tables with FKs
ALTER TABLE transactions ALTER COLUMN blockhash TYPE HASH_TYPE USING DECODE(blockhash, 'hex');
ALTER TABLE block_address_gcs ALTER COLUMN blockhash TYPE HASH_TYPE USING DECODE(blockhash, 'hex');
ALTER TABLE block_synchronization_progress ALTER COLUMN blockhash TYPE HASH_TYPE USING DECODE(blockhash, 'hex');

-- add FKs back
ALTER TABLE transactions ADD CONSTRAINT transactions_blockhash_fk FOREIGN KEY (blockhash) REFERENCES blocks(blockhash);
ALTER TABLE block_address_gcs ADD CONSTRAINT block_address_gcs_blockhash_fk FOREIGN KEY (blockhash) REFERENCES blocks(blockhash);

-- drop unused type
DROP DOMAIN BLOCKHASH_TYPE;

# --- !Downs

-- add domain
CREATE DOMAIN BLOCKHASH_TYPE AS TEXT
CHECK (
  VALUE ~ '^[a-f0-9]{64}$'
);

-- drop FK constraints to be able to update column types one after another
ALTER TABLE transactions DROP CONSTRAINT transactions_blockhash_fk;
ALTER TABLE block_address_gcs DROP CONSTRAINT block_address_gcs_blockhash_fk;

-- update main types
ALTER TABLE blocks
    ALTER COLUMN blockhash TYPE BLOCKHASH_TYPE USING ENCODE(blockhash, 'hex'),
    ALTER COLUMN previous_blockhash TYPE BLOCKHASH_TYPE USING ENCODE(previous_blockhash, 'hex'),
    ALTER COLUMN next_blockhash TYPE BLOCKHASH_TYPE USING ENCODE(next_blockhash, 'hex');

-- update tables with FKs
ALTER TABLE transactions ALTER COLUMN blockhash TYPE BLOCKHASH_TYPE USING ENCODE(blockhash, 'hex');
ALTER TABLE block_address_gcs ALTER COLUMN blockhash TYPE BLOCKHASH_TYPE USING ENCODE(blockhash, 'hex');
ALTER TABLE block_synchronization_progress ALTER COLUMN blockhash TYPE BLOCKHASH_TYPE USING ENCODE(blockhash, 'hex');

-- add FKs back
ALTER TABLE transactions ADD CONSTRAINT transactions_blockhash_fk FOREIGN KEY (blockhash) REFERENCES blocks(blockhash);
ALTER TABLE block_address_gcs ADD CONSTRAINT block_address_gcs_blockhash_fk FOREIGN KEY (blockhash) REFERENCES blocks(blockhash);
