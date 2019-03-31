
# --- !Ups

CREATE TYPE TPOS_CONTRACT_STATE AS ENUM ('ACTIVE', 'CLOSED');

-- a contract is created using a 1 XSN which is referenced by the (txid, index).
-- spending the collateral coin cancels the contract,
CREATE TABLE tpos_contracts(
  txid TXID_TYPE NOT NULL,
  index NON_NEGATIVE_INT_TYPE NOT NULL,
  owner ADDRESS_TYPE NOT NULL,
  merchant ADDRESS_TYPE NOT NULL,
  merchant_commission INT NOT NULL,
  time BIGINT NOT NULL,
  state TPOS_CONTRACT_STATE NOT NULL,
  closed_on TXID_TYPE NULL,
  -- constraints
  CONSTRAINT tpos_contracts_txid_index_pk PRIMARY KEY (txid, index),
  CONSTRAINT tpos_contracts_txid_index_fk FOREIGN KEY (txid, index) REFERENCES transaction_outputs (txid, index),
  CONSTRAINT merchant_commission_percentage CHECK (merchant_commission > 0 AND merchant_commission < 100),
  CONSTRAINT tpos_contracts_closed_on_fk FOREIGN KEY (closed_on) REFERENCES transactions (txid) ON DELETE SET NULL
);

CREATE INDEX tpos_contracts_owner_index ON tpos_contracts USING BTREE (owner);
CREATE INDEX tpos_contracts_merchant_index ON tpos_contracts USING BTREE (merchant);
CREATE INDEX tpos_contracts_closed_on_index ON tpos_contracts USING BTREE (closed_on);


# --- !Downs

DROP INDEX tpos_contracts_closed_on_index;
DROP INDEX tpos_contracts_merchant_index;
DROP INDEX tpos_contracts_owner_index;
DROP TABLE tpos_contracts;
DROP TYPE TPOS_CONTRACT_STATE;

