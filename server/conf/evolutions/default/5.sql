-- As bech32 format is not supported, it is simpler to just disable the checks until it is supported.

# --- !Ups

ALTER DOMAIN ADDRESS_TYPE DROP CONSTRAINT address_type_check;
ALTER DOMAIN ADDRESS_TYPE ADD CONSTRAINT address_type_check CHECK (VALUE <> '');

# --- !Downs

ALTER DOMAIN ADDRESS_TYPE DROP CONSTRAINT address_type_check;
ALTER DOMAIN ADDRESS_TYPE ADD CONSTRAINT address_type_check CHECK (VALUE ~ '(^[A-Za-z0-9]{34}$)|(^[A-Za-z0-9]{42}$)');
