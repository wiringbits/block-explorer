
# --- !Ups

CREATE TABLE aggregated_amounts(
  name TEXT NOT NULL,
  value AMOUNT_TYPE NOT NULL,
  -- constraints
  CONSTRAINT aggregated_amounts_name_pk PRIMARY KEY (name)
);

INSERT INTO aggregated_amounts
SELECT 'available_coins' AS name, COALESCE(SUM(received - spent), 0) AS value FROM balances;


# --- !Downs

DROP TABLE aggregated_amounts;
