
# --- !Ups

-- we pre-compute the balances while storing transactions in order to perform
-- simpler queries while requiring addresses and the available amounts.
CREATE TABLE balances(
  address ADDRESS_TYPE NOT NULL,
  received DECIMAL(30, 15) NOT NULL,
  spent DECIMAL(30, 15) NOT NULL,
  -- constraints
  CONSTRAINT balances_address_pk PRIMARY KEY (address)
);

CREATE INDEX balances_available_index ON balances USING BTREE ((received - spent));


-- there are certain addresses that we need to hide from the public,
-- like the one used for the coin swap.
CREATE TABLE hidden_addresses(
  address ADDRESS_TYPE NOT NULL,
  -- constraints
  CONSTRAINT hidden_addresses_address_pk PRIMARY KEY (address)
);


# --- !Downs

DROP TABLE hidden_addresses;
DROP INDEX balances_available_index;
DROP TABLE balances;
