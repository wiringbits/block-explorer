
# --- !Ups

ALTER TABLE transaction_outputs
    ALTER COLUMN address TYPE TEXT[] USING ARRAY[address::TEXT];
ALTER TABLE transaction_outputs
    RENAME address TO addresses;


ALTER TABLE transaction_inputs
    ALTER COLUMN address TYPE TEXT[] USING ARRAY[address::TEXT];
ALTER TABLE transaction_inputs
    RENAME address TO addresses;


# --- !Downs

ALTER TABLE transaction_outputs
    ALTER COLUMN addresses TYPE ADDRESS_TYPE USING addresses[1]::ADDRESS_TYPE;
ALTER TABLE transaction_outputs
    RENAME addresses TO address;

ALTER TABLE transaction_inputs
    ALTER COLUMN addresses TYPE ADDRESS_TYPE USING addresses[1]::ADDRESS_TYPE;
ALTER TABLE transaction_inputs
    RENAME addresses TO address;
