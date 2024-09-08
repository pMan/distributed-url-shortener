-- list DBs
\l

-- connect to DB dus
\c dus

-- SCHEMA: dus_schema
CREATE SCHEMA IF NOT EXISTS dus_schema
    AUTHORIZATION dus;

-- Table: dus_schema.url_mapping
CREATE TABLE IF NOT EXISTS dus_schema.url_mapping
(
    hash character varying NOT NULL,
    url character varying NOT NULL,
    CONSTRAINT url_mapping_pkey PRIMARY KEY (hash)
);

-- Index: on url_mapping
CREATE INDEX IF NOT EXISTS url_index
    ON dus_schema.url_mapping USING btree
    (url ASC NULLS LAST)
    INCLUDE(hash)
    TABLESPACE pg_default;

-- example INSERT
INSERT INTO dus_schema.url_mapping(
	hash, url)
	VALUES ('j6A', 'https://www.wikipedia.org/');

-- example SELECT
SELECT hash, url FROM dus_schema.url_mapping;
