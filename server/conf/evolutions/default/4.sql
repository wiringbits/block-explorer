
# --- !Ups

DROP INDEX blocks_height_index;

ALTER TABLE blocks
ADD CONSTRAINT blocks_height_unique UNIQUE (height);

# --- !Downs

ALTER TABLE blocks
DROP CONSTRAINT blocks_height_unique;

CREATE INDEX blocks_height_index ON blocks USING BTREE (height);
