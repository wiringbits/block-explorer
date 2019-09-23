
# --- !Ups

CREATE TABLE block_rewards(
  blockhash HASH_TYPE NOT NULL,
  address ADDRESS_TYPE NOT NULL,
  value AMOUNT_TYPE NOT NULL,
  -- constraints
  CONSTRAINT block_rewards_pk PRIMARY KEY (blockhash, address)
);


# --- !Downs

DROP TABLE blocks;
