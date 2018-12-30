
# --- !Ups

ALTER TABLE transactions
ADD COLUMN index NON_NEGATIVE_INT_TYPE NULL DEFAULT NULL;


# --- !Downs

ALTER TABLE transactions
DROP COLUMN index;
