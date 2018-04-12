
# --- !Ups

CREATE TABLE blocks(
  blockhash VARCHAR(64) NOT NULL,
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
  CONSTRAINT blocks_blockhash_pk PRIMARY KEY (blockhash),
  CONSTRAINT blocks_blockhash_format CHECK (blockhash ~ '^[a-f0-9]{64}$'),
  CONSTRAINT blocks_previous_blockhash_format CHECK (previous_blockhash ~ '^[a-f0-9]{64}$'),
  CONSTRAINT blocks_next_blockhash_format CHECK (next_blockhash ~ '^[a-f0-9]{64}$'),
  CONSTRAINT blocks_tpos_contract_format CHECK (tpos_contract ~ '^[a-f0-9]{64}$')
);

CREATE INDEX blocks_height_index ON blocks USING BTREE (height);
CREATE INDEX blocks_time_index ON blocks USING BTREE (time);

# --- !Downs

DROP TABLE blocks;
