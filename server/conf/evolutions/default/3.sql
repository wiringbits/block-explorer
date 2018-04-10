
# --- !Ups

CREATE TABLE transactions(
  txid VARCHAR(64) NOT NULL,
  blockhash VARCHAR(64) NOT NULL,
  time BIGINT NOT NULL,
  size INT NOT NULL,
  -- constraints
  CONSTRAINT transactions_txid_pk PRIMARY KEY (txid),
  CONSTRAINT transactions_txid_format CHECK (txid ~ '^[a-f0-9]{64}$'),
  CONSTRAINT transactions_blockhash_format CHECK (blockhash ~ '^[a-f0-9]{64}$')
);

CREATE INDEX transactions_blockhash_index ON transactions (blockhash);


CREATE TABLE transaction_inputs(
  txid VARCHAR(64) NOT NULL,
  index INT NOT NULL,
  value DECIMAL(30, 15) NULL,
  address VARCHAR(34) NULL,
  -- constraints
  CONSTRAINT transaction_inputs_txid_index_pk PRIMARY KEY (txid, index),
  CONSTRAINT transaction_inputs_txid_format CHECK (txid ~ '^[a-f0-9]{64}$'),
  CONSTRAINT transaction_inputs_address_format CHECK (address ~ '[a-zA-Z0-9]{34}$')
);

CREATE TABLE transaction_outputs(
  txid VARCHAR(64) NOT NULL,
  index INT NOT NULL,
  value DECIMAL(30, 15) NOT NULL,
  address VARCHAR(34) NULL,
  tpos_owner_address VARCHAR(34) NULL,
  tpos_merchant_address VARCHAR(34) NULL,
  -- constraints
  CONSTRAINT transaction_outputs_txid_index_pk PRIMARY KEY (txid, index),
  CONSTRAINT transaction_outputs_txid_format CHECK (txid ~ '^[a-f0-9]{64}$'),
  CONSTRAINT transaction_outputs_address_format CHECK (address ~ '[a-zA-Z0-9]{34}$'),
  CONSTRAINT transaction_outputs_tpos_owner_address_format CHECK (address ~ '[a-zA-Z0-9]{34}$'),
  CONSTRAINT transaction_outputs_tpos_merchant_address_format CHECK (address ~ '[a-zA-Z0-9]{34}$')
);


# --- !Downs

DROP TABLE transaction_outputs;
DROP TABLE transaction_inputs;
DROP TABLE transactions;
