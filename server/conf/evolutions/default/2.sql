
# --- !Ups

-- we pre-compute the balances while storing transactions in order to perform
-- simpler queries while requiring addresses and the available amounts.
CREATE TABLE balances(
  address VARCHAR(34) NOT NULL,
  received DECIMAL(30, 15) NOT NULL,
  spent DECIMAL(30, 15) NOT NULL,
  available DECIMAL(30, 15) NOT NULL,
  -- constraints
  CONSTRAINT balances_address_pk PRIMARY KEY (address),
  CONSTRAINT balances_address_format CHECK (address ~ '[a-zA-Z0-9]{34}$')
);

CREATE INDEX balances_available_index ON balances (available);


-- there are certain addresses that we need to hide from public, like the one
-- used for the coin swap.
CREATE TABLE hidden_addresses(
  address VARCHAR(34) NOT NULL,
  -- constraints
  CONSTRAINT hidden_addresses_address_pk PRIMARY KEY (address),
  CONSTRAINT hidden_addresses_address_format CHECK (address ~ '[a-zA-Z0-9]{34}$')
);


# --- !Downs

DROP TABLE hidden_addresses;
DROP TABLE balances;
