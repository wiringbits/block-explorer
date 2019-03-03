
# --- !Ups

ALTER TABLE blocks
    ALTER COLUMN difficulty TYPE DECIMAL(35, 20);


# --- !Downs

ALTER TABLE blocks
    ALTER COLUMN difficulty TYPE DECIMAL(30, 20);
