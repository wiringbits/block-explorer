
-- query transactions >= 15000 XSN on a given date interval
SELECT encode(txid, 'hex') as txid, (sent - received) as amount, to_timestamp(time) as time
FROM address_transaction_details
WHERE (sent - received) >= 15000 AND
  to_timestamp(time)::DATE >= DATE '2020-12-01' AND
  to_timestamp(time)::DATE <= DATE '2020-12-23'
ORDER BY (sent - received) desc;

-- avg time to get a new block
SELECT AVG(b.time - a.time) AS new_block_avg_time
FROM (SELECT height, time FROM blocks) a JOIN
     (SELECT height, time FROM blocks) b ON (a.height + 1 = b.height);

-- print the number of transactions per block and count how many blocks have that number
SELECT txcount, COUNT(*) AS times
FROM (
  SELECT COUNT(txid) AS txcount, height
  FROM blocks LEFT JOIN transactions USING (blockhash)
  GROUP BY height) tmp
GROUP BY txcount
ORDER BY txcount;

-- find corrupted balances
SELECT address, one.available AS one, (two.received - two.spent) AS two
FROM (
    SELECT address, received - spent AS available
    FROM (
        SELECT addresses[1] AS address, SUM(value) AS spent
        FROM transaction_inputs
        WHERE array_length(addresses, 1) > 0
        GROUP BY addresses[1]
      ) s JOIN
      (
        SELECT addresses[1] AS address, SUM(value) AS received
        FROM transaction_outputs
        WHERE array_length(addresses, 1) > 0
        GROUP BY addresses[1]
      ) r USING (address)
  ) one JOIN balances two USING (address)
WHERE one.available <> (two.received - two.spent)
ORDER BY two.received
LIMIT 10;

-- rebuild existing balances from the address_transaction_details table
UPDATE balances b
SET received = (
  SELECT COALESCE(SUM(received), 0) AS received
  FROM address_transaction_details
  WHERE address = b.address
), spent = (
  SELECT COALESCE(SUM(sent), 0) AS received
  FROM address_transaction_details
  WHERE address = b.address
);
