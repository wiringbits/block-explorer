
# --- !Ups

CREATE TABLE address_transaction_details (
  address ADDRESS_TYPE NOT NULL,
  txid TXID_TYPE NOT NULL,
  sent AMOUNT_TYPE NOT NULL,
  received AMOUNT_TYPE NOT NULL,
  time BIGINT NOT NULL, -- it is cheaper to carry this value from the tx than to use joins in each query
  -- constraints
  CONSTRAINT address_transaction_details_pk PRIMARY KEY (address, txid),
  CONSTRAINT address_transaction_details_txid_fk FOREIGN KEY (txid) REFERENCES transactions (txid)
);

CREATE INDEX address_transaction_details_txid_index ON address_transaction_details USING BTREE (txid);
CREATE INDEX address_transaction_details_time_index ON address_transaction_details USING BTREE (time);

# --- !Downs

DROP INDEX address_transaction_details_time_index;
DROP INDEX address_transaction_details_txid_index;
DROP TABLE address_transaction_details;
