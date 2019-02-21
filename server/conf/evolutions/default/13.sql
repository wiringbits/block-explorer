
# --- !Ups

CREATE INDEX IF NOT EXISTS transaction_inputs_from_index ON transaction_inputs USING BTREE (from_txid, from_output_index);


# --- !Downs

DROP INDEX transaction_inputs_from_index;
