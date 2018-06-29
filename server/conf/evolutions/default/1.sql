
# --- !Ups

-- custom types are defined to simplify the validations and simplify updates to the validations.

CREATE DOMAIN BLOCKHASH_TYPE AS TEXT
CHECK (
  VALUE ~ '^[a-f0-9]{64}$'
);

CREATE DOMAIN TXID_TYPE AS TEXT
CHECK (
  VALUE ~ '^[a-f0-9]{64}$'
);

CREATE DOMAIN ADDRESS_TYPE AS TEXT
CHECK (
  VALUE ~ '(^[A-Za-z0-9]{34}$)|(^[A-Za-z0-9]{42}$)'
);

CREATE DOMAIN NON_NEGATIVE_INT_TYPE AS INT
CHECK (
  VALUE >= 0
);

# --- !Downs

DROP DOMAIN NON_NEGATIVE_INT_TYPE;
DROP DOMAIN ADDRESS_TYPE;
DROP DOMAIN TXID_TYPE;
DROP DOMAIN BLOCKHASH_TYPE;
