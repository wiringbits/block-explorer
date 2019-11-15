
# --- !Ups
ALTER TABLE block_rewards
    ADD COLUMN staked_amount AMOUNT_TYPE,
    ADD COLUMN staked_time BIGINT;

# --- !Downs
ALTER TABLE block_rewards
    DROP COLUMN staked_amount,
    DROP COLUMN staked_time;
