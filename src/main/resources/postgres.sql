-- Database: dus

-- DROP DATABASE IF EXISTS dus;

CREATE DATABASE dus
    WITH
    OWNER = dus
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;

-- Role: dus
-- DROP ROLE IF EXISTS dus;

CREATE ROLE dus WITH
  LOGIN
  NOSUPERUSER
  INHERIT
  NOCREATEDB
  NOCREATEROLE
  NOREPLICATION
  ENCRYPTED PASSWORD 'SCRAM-SHA-256$4096:UuaHLUgebeBlZ89ro6TJmg==$hlgqJrVnF3B4myS9tsFZSxtJtZgDAg8LXT0yjUxYRwA=:yIxbNer0/eymXISBSS+Vzc4KjIQvvemKSXweflqndWQ=';

COMMENT ON ROLE dus IS 'user for distributed URL shortener';

ALTER USER dus WITH PASSWORD 'dus123';

-- SCHEMA: dus_schema

-- DROP SCHEMA IF EXISTS dus_schema ;

CREATE SCHEMA IF NOT EXISTS dus_schema
    AUTHORIZATION dus;

COMMENT ON SCHEMA dus_schema
    IS 'schema for DUS core tables';

-- Table: dus_schema.url_mapping

-- DROP TABLE IF EXISTS dus_schema.url_mapping;

CREATE TABLE IF NOT EXISTS dus_schema.url_mapping
(
    hash character varying NOT NULL,
    url character varying NOT NULL,
    CONSTRAINT url_mapping_pkey PRIMARY KEY (hash)
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS dus_schema.url_mapping
    OWNER to dus;
-- Index: url_index

-- DROP INDEX IF EXISTS dus_schema.url_index;

CREATE INDEX IF NOT EXISTS url_index
    ON dus_schema.url_mapping USING btree
    (url ASC NULLS LAST)
    INCLUDE(hash)
    TABLESPACE pg_default;

-- example INSERT
INSERT INTO dus_schema.url_mapping(
	hash, url)
	VALUES ('j6A', 'https://wikipedia.org');

-- example SELECT
SELECT hash, url FROM dus_schema.url_mapping;

