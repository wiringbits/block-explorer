# --- !Ups

-- old rows need to be calculated, after migration is completed this can be NOT NULL
ALTER TABLE transactions ADD COLUMN sent AMOUNT_TYPE NULL;
ALTER TABLE transactions ADD COLUMN received AMOUNT_TYPE NULL;

CREATE INDEX transactions_sent_index ON transactions(sent);
CREATE INDEX transactions_received_index ON transactions(received);