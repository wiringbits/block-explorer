
# --- !Ups

CREATE TABLE blocks(
  hash VARCHAR(64) NOT NULL,
  previous_blockhash VARCHAR(64) NULL,
  next_blockhash VARCHAR(64) NULL,
  merkle_root VARCHAR(64) NULL,
  tpos_contract VARCHAR(64) NULL,
  size INT NOT NULL,
  height INT NOT NULL,
  version INT NOT NULL,
  time BIGINT NOT NULL,
  median_time BIGINT NOT NULL,
  nonce INT NOT NULL,
  bits VARCHAR(50) NOT NULL,
  chainwork VARCHAR(80) NOT NULL,
  difficulty DECIMAL(30, 20),
  -- constraints
  CONSTRAINT blocks_hash_pk PRIMARY KEY (hash)
);

CREATE INDEX blocks_height_index ON blocks USING BTREE (height);
CREATE INDEX blocks_time_index ON blocks USING BTREE (time);

# --- !Downs

DROP TABLE blocks;
