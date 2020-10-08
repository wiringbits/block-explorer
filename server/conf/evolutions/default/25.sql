# --- !Ups

-- The tpos commission percent changed from 1-99 to 0-100
ALTER TABLE tpos_contracts DROP CONSTRAINT merchant_commission_percentage;
ALTER TABLE tpos_contracts ADD CONSTRAINT merchant_commission_percentage CHECK (merchant_commission >= 0 AND merchant_commission <= 100);

# --- !Downs
ALTER TABLE tpos_contracts DROP CONSTRAINT merchant_commission_percentage;
ALTER TABLE tpos_contracts ADD CONSTRAINT merchant_commission_percentage CHECK (merchant_commission > 0 AND merchant_commission < 100);