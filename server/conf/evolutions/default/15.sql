
# --- !Ups

CREATE DOMAIN HEX_STRING_TYPE AS TEXT
CHECK (
  VALUE ~ '^([a-f0-9][a-f0-9])+$'
);

-- a golomb-coded sets filter to query for the block addresses
CREATE TABLE block_address_gcs(
  blockhash BLOCKHASH_TYPE NOT NULL,
  p NON_NEGATIVE_INT_TYPE NOT NULL,
  n NON_NEGATIVE_INT_TYPE NOT NULL,
  m NON_NEGATIVE_INT_TYPE NOT NULL,
  hex HEX_STRING_TYPE NOT NULL,
  -- constraints
  CONSTRAINT block_address_gcs_blockhash_pk PRIMARY KEY (blockhash),
  CONSTRAINT block_address_gcs_blockhash_fk FOREIGN KEY (blockhash) REFERENCES blocks (blockhash)
);


# --- !Downs

DROP TABLE block_address_gcs;
DROP DOMAIN HEX_STRING_TYPE;
