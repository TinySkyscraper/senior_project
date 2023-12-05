CREATE TABLE auth(
    username text,
    pubkey bytea,
    doors text
);
CREATE TABLE admins(
    username text,
    password text,
    token text,
    expiration bigint
);

INSERT INTO admins VALUES('kmoseley','pass',NULL,NULL);