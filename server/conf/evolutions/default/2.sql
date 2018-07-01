
# --- !Ups

CREATE TABLE blocks(
  blockhash BLOCKHASH_TYPE NOT NULL,
  previous_blockhash BLOCKHASH_TYPE NULL,
  next_blockhash BLOCKHASH_TYPE NULL,
  merkle_root TEXT NOT NULL,
  tpos_contract TXID_TYPE NULL,
  size NON_NEGATIVE_INT_TYPE NOT NULL,
  height NON_NEGATIVE_INT_TYPE NOT NULL,
  version INT NOT NULL,
  time BIGINT NOT NULL,
  median_time BIGINT NOT NULL,
  nonce INT NOT NULL,
  bits TEXT NOT NULL,
  chainwork TEXT NOT NULL,
  difficulty DECIMAL(30, 20) NOT NULL,
  -- constraints
  CONSTRAINT blocks_blockhash_pk PRIMARY KEY (blockhash),
  CONSTRAINT blocks_height_unique UNIQUE (height),
  CONSTRAINT blocks_previous_blockhash_fk FOREIGN KEY (previous_blockhash) REFERENCES blocks (blockhash),
  CONSTRAINT blocks_next_blockhash_fk FOREIGN KEY (next_blockhash) REFERENCES blocks (blockhash) ON DELETE SET NULL
);

CREATE INDEX blocks_time_index ON blocks USING BTREE (time);


# --- !Downs

DROP INDEX blocks_time_index;
DROP TABLE blocks;
