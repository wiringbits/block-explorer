
# --- !Ups

ALTER TABLE transactions
    ALTER COLUMN index SET NOT NULL,
    ALTER COLUMN index DROP DEFAULT;


# --- !Downs

ALTER TABLE transactions
    ALTER COLUMN index DROP NOT NULL,
    ALTER COLUMN index SET DEFAULT NULL;
