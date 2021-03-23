# --- !Ups

-- old rows need to be calculated, after migration is completed this can be NOT NULL
ALTER TABLE transactions ADD COLUMN sent AMOUNT_TYPE NULL;
ALTER TABLE transactions ADD COLUMN received AMOUNT_TYPE NULL;

CREATE INDEX transactions_sent_plus_received_index ON transactions((sent + received));