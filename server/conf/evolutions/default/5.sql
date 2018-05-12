
# --- !Ups

DROP INDEX balances_available_index;

ALTER TABLE balances
DROP COLUMN available;

CREATE INDEX balances_available_index ON balances ((received - spent));


# --- !Downs

DROP INDEX balances_available_index;

-- in case this down is applied, the table needs to be rebuilt.
ALTER TABLE balances
ADD COLUMN available DECIMAL(30, 15) NOT NULL DEFAULT 0;

CREATE INDEX balances_available_index ON balances (available);
