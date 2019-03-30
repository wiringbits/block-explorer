
# --- !Ups

ALTER TABLE transaction_outputs
    DROP COLUMN tpos_owner_address,
    DROP COLUMN tpos_merchant_address;


# --- !Downs

ALTER TABLE blocks
    ADD COLUMN tpos_owner_address ADDRESS_TYPE NULL,
    ADD COLUMN tpos_merchant_address ADDRESS_TYPE NULL;
