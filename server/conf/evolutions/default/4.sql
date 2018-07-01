
# --- !Ups

CREATE TABLE transactions(
  txid TXID_TYPE NOT NULL,
  blockhash BLOCKHASH_TYPE NOT NULL,
  time BIGINT NOT NULL,
  size NON_NEGATIVE_INT_TYPE NOT NULL,
  -- constraints
  CONSTRAINT transactions_txid_pk PRIMARY KEY (txid),
  CONSTRAINT transactions_blockhash_fk FOREIGN KEY (blockhash) REFERENCES blocks (blockhash)
);

CREATE INDEX transactions_blockhash_index ON transactions USING BTREE (blockhash);
CREATE INDEX transactions_time_index ON transactions USING BTREE (time);

-- TODO: it might be worth to add a unique constraint based for (from_txid, from_output_index)
CREATE TABLE transaction_inputs(
  txid TXID_TYPE NOT NULL,
  index NON_NEGATIVE_INT_TYPE NOT NULL,
  from_txid TXID_TYPE NOT NULL,
  from_output_index NON_NEGATIVE_INT_TYPE NOT NULL,
  value DECIMAL(30, 15) NULL,
  address ADDRESS_TYPE NULL,
  -- constraints
  CONSTRAINT transaction_inputs_txid_index_pk PRIMARY KEY (txid, index),
  CONSTRAINT transaction_inputs_txid_fk FOREIGN KEY (txid) REFERENCES transactions (txid),
  CONSTRAINT transaction_inputs_from_txid_fk FOREIGN KEY (from_txid) REFERENCES transactions (txid)
);

CREATE INDEX transaction_inputs_address_index ON transaction_inputs USING BTREE (address);


CREATE TABLE transaction_outputs(
  txid TXID_TYPE NOT NULL,
  index NON_NEGATIVE_INT_TYPE NOT NULL,
  value DECIMAL(30, 15) NOT NULL,
  address ADDRESS_TYPE NOT NULL,
  hex_script TEXT NOT NULL,
  spent_on TXID_TYPE NULL,
  tpos_owner_address ADDRESS_TYPE NULL,
  tpos_merchant_address ADDRESS_TYPE NULL,
  -- constraints
  CONSTRAINT transaction_outputs_txid_index_pk PRIMARY KEY (txid, index),
  CONSTRAINT transaction_outputs_txid_fk FOREIGN KEY (txid) REFERENCES transactions (txid),
  CONSTRAINT transaction_outputs_spent_on_fk FOREIGN KEY (spent_on) REFERENCES transactions (txid) ON DELETE SET NULL
);

CREATE INDEX transaction_outputs_address_index ON transaction_outputs USING BTREE (address);
CREATE INDEX transaction_outputs_spent_on ON transaction_outputs USING BTREE (spent_on) WHERE spent_on IS NULL;

# --- !Downs

DROP INDEX transaction_outputs_address_index;
DROP TABLE transaction_outputs;

DROP INDEX transaction_inputs_address_index;
DROP TABLE transaction_inputs;

DROP INDEX transactions_blockhash_index;
DROP INDEX transactions_time_index;
DROP TABLE transactions;
