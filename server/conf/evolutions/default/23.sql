
# --- !Ups
ALTER TYPE BLOCK_SYNCHRONIZATION_STATE ADD VALUE 'STORING_REWARDS';

CREATE TYPE REWARD_TYPE AS ENUM (
  'PoW',
  'PoS',
  'TPoS_OWNER',
  'TPoS_MERCHANT'
);

CREATE TABLE block_rewards(
  blockhash HASH_TYPE NOT NULL,
  value AMOUNT_TYPE NOT NULL,
  address ADDRESS_TYPE NOT NULL,
  type REWARD_TYPE NOT NULL,
  -- constraints
  CONSTRAINT block_rewards_pk PRIMARY KEY (blockhash, type),
  CONSTRAINT block_rewards_blockhash_fk FOREIGN KEY (blockhash) REFERENCES blocks (blockhash)
);

CREATE INDEX block_rewards_address_index ON block_rewards USING BTREE (address);


# --- !Downs

DROP INDEX block_rewards_address_index;
DROP TABLE block_rewards;
DROP TYPE REWARD_TYPE;
