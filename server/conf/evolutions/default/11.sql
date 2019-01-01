
# --- !Ups

CREATE INDEX blocks_next_blockhash_index ON blocks USING BTREE (next_blockhash);
CREATE INDEX blocks_previous_blockhash_index ON blocks USING BTREE (previous_blockhash);

DROP INDEX transaction_outputs_spent_on;
CREATE INDEX transaction_outputs_spent_on_index ON transaction_outputs USING BTREE (spent_on);


# --- !Downs

DROP INDEX transaction_outputs_spent_on_index;
CREATE INDEX transaction_outputs_spent_on ON transaction_outputs USING BTREE (spent_on);

DROP INDEX blocks_previous_blockhash_index;
DROP INDEX blocks_next_blockhash_index;
