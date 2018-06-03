-- clear data
delete from blocks; delete from transactions; delete from transaction_inputs; delete from transaction_outputs; delete from balances;

-- avg time to get a new block
SELECT AVG(b.time - a.time) AS new_block_avg_time
FROM (SELECT height, time FROM blocks) a JOIN
     (SELECT height, time FROM blocks) b ON (a.height + 1 = b.height);


-- find blocks with corrupted next_blockhash
SELECT height, blockhash, next_blockhash
FROM blocks b
WHERE 0 = (SELECT COUNT(*) FROM blocks WHERE blockhash = b.next_blockhash) AND
      height < (SELECT MAX(height) FROM blocks);

-- find blocks with corrupted previous_blockhash
SELECT height, blockhash, previous_blockhash
FROM blocks b
WHERE 0 = (SELECT COUNT(*) FROM blocks WHERE blockhash = b.previous_blockhash) AND
      height > (SELECT MIN(height) FROM blocks);

-- find missing blocks in the chain
SELECT height - 1 AS missing
FROM blocks b
WHERE height > 1 AND
      height - 1 NOT IN (
  SELECT height
  FROM blocks
  WHERE height = b.height - 1
);

-- find corrupted balances
SELECT address, one.available AS one, (two.received - two.spent) AS two
FROM (
    SELECT address, received - spent AS available
    FROM (
        SELECT address, SUM(value) AS spent
        FROM transaction_inputs
        GROUP BY address
      ) s JOIN
      (
        SELECT address, SUM(value) AS received
        FROM transaction_outputs
        GROUP BY address
      ) r USING (address)
  ) one JOIN balances two USING (address)
WHERE one.available <> (two.received - two.spent);
--

-- rebuild balances table
-- 1. count number of balances
SELECT COUNT(*)
FROM balances;

-- 2. verify you would write the same amount
SELECT COUNT(*)
FROM
  (
    SELECT address, SUM(value) AS received
    FROM transaction_outputs
    GROUP BY address
  ) r LEFT JOIN (
      SELECT address, SUM(value) AS spent
      FROM transaction_inputs
      GROUP BY address
  ) s USING (address);

-- 3. delete balances, be sure that the explorer is turned off
DELETE FROM balances;

-- 4. insert the balances
INSERT INTO balances
  (
    SELECT address, received, COALESCE(spent, 0) AS spent
    FROM
      (
        SELECT address, SUM(value) AS received
        FROM transaction_outputs
        GROUP BY address
      ) r LEFT JOIN (
          SELECT address, SUM(value) AS spent
          FROM transaction_inputs
          GROUP BY address
      ) s USING (address)
  );

-- 5. verify you have the same amount
SELECT COUNT(*) FROM balances;

-- 6. start explorer
