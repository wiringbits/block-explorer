
# --- !Ups

-- drop FK constraints to be able to update column types one after another
ALTER TABLE address_transaction_details DROP CONSTRAINT address_transaction_details_txid_fk;

ALTER TABLE tpos_contracts
    DROP CONSTRAINT tpos_contracts_txid_index_fk,
    DROP CONSTRAINT tpos_contracts_closed_on_fk;

ALTER TABLE transaction_inputs
    DROP CONSTRAINT transaction_inputs_txid_fk,
    DROP CONSTRAINT transaction_inputs_from_fk;

ALTER TABLE transaction_outputs
    DROP CONSTRAINT transaction_outputs_spent_on_fk,
    DROP CONSTRAINT transaction_outputs_txid_fk;

-- update main types
ALTER TABLE transactions ALTER COLUMN txid TYPE HASH_TYPE USING DECODE(txid, 'hex');

-- update tables with FKs
ALTER TABLE address_transaction_details ALTER COLUMN txid TYPE HASH_TYPE USING DECODE(txid, 'hex');
ALTER TABLE blocks ALTER COLUMN tpos_contract TYPE HASH_TYPE USING DECODE(tpos_contract, 'hex');

ALTER TABLE tpos_contracts
    ALTER COLUMN txid TYPE HASH_TYPE USING DECODE(txid, 'hex'),
    ALTER COLUMN closed_on TYPE HASH_TYPE USING DECODE(closed_on, 'hex');

ALTER TABLE transaction_inputs
    ALTER COLUMN txid TYPE HASH_TYPE USING DECODE(txid, 'hex'),
    ALTER COLUMN from_txid TYPE HASH_TYPE USING DECODE(from_txid, 'hex');

ALTER TABLE transaction_outputs
    ALTER COLUMN spent_on TYPE HASH_TYPE USING DECODE(spent_on, 'hex'),
    ALTER COLUMN txid TYPE HASH_TYPE USING DECODE(txid, 'hex');

-- add FKs back
ALTER TABLE address_transaction_details ADD CONSTRAINT address_transaction_details_txid_fk FOREIGN KEY (txid) REFERENCES transactions(txid);

ALTER TABLE tpos_contracts
    ADD CONSTRAINT tpos_contracts_txid_index_fk FOREIGN KEY (txid, index) REFERENCES transaction_outputs(txid, index),
    ADD CONSTRAINT tpos_contracts_closed_on_fk FOREIGN KEY (closed_on) REFERENCES transactions(txid) ON DELETE SET NULL;

ALTER TABLE transaction_inputs
    ADD CONSTRAINT transaction_inputs_txid_fk FOREIGN KEY (txid) REFERENCES transactions(txid),
    ADD CONSTRAINT transaction_inputs_from_fk FOREIGN KEY (from_txid, from_output_index) REFERENCES transaction_outputs(txid, index);

ALTER TABLE transaction_outputs
    ADD CONSTRAINT transaction_outputs_spent_on_fk FOREIGN KEY (spent_on) REFERENCES transactions(txid) ON DELETE SET NULL,
    ADD CONSTRAINT transaction_outputs_txid_fk FOREIGN KEY (txid) REFERENCES transactions(txid);

-- drop unused type
DROP DOMAIN TXID_TYPE;


# --- !Downs

-- add domain
CREATE DOMAIN TXID_TYPE AS TEXT
CHECK (
  VALUE ~ '^[a-f0-9]{64}$'
);

-- drop FK constraints to be able to update column types one after another
ALTER TABLE address_transaction_details DROP CONSTRAINT address_transaction_details_txid_fk;

ALTER TABLE tpos_contracts
    DROP CONSTRAINT tpos_contracts_txid_index_fk,
    DROP CONSTRAINT tpos_contracts_closed_on_fk;

ALTER TABLE transaction_inputs
    DROP CONSTRAINT transaction_inputs_txid_fk,
    DROP CONSTRAINT transaction_inputs_from_fk;

ALTER TABLE transaction_outputs
    DROP CONSTRAINT transaction_outputs_spent_on_fk,
    DROP CONSTRAINT transaction_outputs_txid_fk;

-- update main types
ALTER TABLE transactions ALTER COLUMN txid TYPE TXID_TYPE USING ENCODE(txid, 'hex');

-- update tables with FKs
ALTER TABLE address_transaction_details ALTER COLUMN txid TYPE TXID_TYPE USING ENCODE(txid, 'hex');
ALTER TABLE blocks ALTER COLUMN tpos_contract TYPE TXID_TYPE USING ENCODE(tpos_contract, 'hex');

ALTER TABLE tpos_contracts
    ALTER COLUMN txid TYPE TXID_TYPE USING ENCODE(txid, 'hex'),
    ALTER COLUMN closed_on TYPE TXID_TYPE USING ENCODE(closed_on, 'hex');

ALTER TABLE transaction_inputs
    ALTER COLUMN txid TYPE TXID_TYPE USING ENCODE(txid, 'hex'),
    ALTER COLUMN from_txid TYPE TXID_TYPE USING ENCODE(from_txid, 'hex');

ALTER TABLE transaction_outputs
    ALTER COLUMN spent_on TYPE TXID_TYPE USING ENCODE(spent_on, 'hex'),
    ALTER COLUMN txid TYPE TXID_TYPE USING ENCODE(txid, 'hex');


-- add FKs back
ALTER TABLE address_transaction_details ADD CONSTRAINT address_transaction_details_txid_fk FOREIGN KEY (txid) REFERENCES transactions(txid);

ALTER TABLE tpos_contracts
    ADD CONSTRAINT tpos_contracts_txid_index_fk FOREIGN KEY (txid, index) REFERENCES transaction_outputs(txid, index),
    ADD CONSTRAINT tpos_contracts_closed_on_fk FOREIGN KEY (closed_on) REFERENCES transactions(txid) ON DELETE SET NULL;

ALTER TABLE transaction_inputs
    ADD CONSTRAINT transaction_inputs_txid_fk FOREIGN KEY (txid) REFERENCES transactions(txid),
    ADD CONSTRAINT transaction_inputs_from_fk FOREIGN KEY (from_txid, from_output_index) REFERENCES transaction_outputs(txid, index);

ALTER TABLE transaction_outputs
    ADD CONSTRAINT transaction_outputs_spent_on_fk FOREIGN KEY (spent_on) REFERENCES transactions(txid) ON DELETE SET NULL,
    ADD CONSTRAINT transaction_outputs_txid_fk FOREIGN KEY (txid) REFERENCES transactions(txid);
