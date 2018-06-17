
# --- !Ups

CREATE INDEX transaction_inputs_address_index ON transaction_inputs (address);

CREATE INDEX transaction_outputs_address_index ON transaction_outputs (address);


# --- !Downs

DROP INDEX transaction_outputs_address_index;

DROP INDEX transaction_inputs_address_index;
