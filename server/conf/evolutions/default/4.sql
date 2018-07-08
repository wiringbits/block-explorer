
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


CREATE TABLE transaction_outputs(
  txid TXID_TYPE NOT NULL,
  index NON_NEGATIVE_INT_TYPE NOT NULL,
  value AMOUNT_TYPE NOT NULL,
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


CREATE TABLE transaction_inputs(
  txid TXID_TYPE NOT NULL,
  index NON_NEGATIVE_INT_TYPE NOT NULL,
  from_txid TXID_TYPE NOT NULL,
  from_output_index NON_NEGATIVE_INT_TYPE NOT NULL,
  value AMOUNT_TYPE NULL,
  address ADDRESS_TYPE NULL,
  -- constraints
  CONSTRAINT transaction_inputs_txid_index_pk PRIMARY KEY (txid, index),
  CONSTRAINT transaction_inputs_txid_fk FOREIGN KEY (txid) REFERENCES transactions (txid),
  CONSTRAINT transaction_inputs_from_fk FOREIGN KEY (from_txid, from_output_index) REFERENCES transaction_outputs (txid, index)
);

CREATE INDEX transaction_inputs_address_index ON transaction_inputs USING BTREE (address);


# --- !Downs

DROP INDEX transaction_inputs_address_index;
DROP TABLE transaction_inputs;

DROP INDEX transaction_outputs_spent_on;
DROP INDEX transaction_outputs_address_index;
DROP TABLE transaction_outputs;

DROP INDEX transactions_time_index;
DROP INDEX transactions_blockhash_index;
DROP TABLE transactions;
