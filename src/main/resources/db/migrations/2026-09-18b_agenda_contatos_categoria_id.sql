-- Converts the EXISTING "contatos_agenda" table from its original free-text
-- "categoria" column to the new "categoria_id" foreign key into
-- "categorias_agenda" (the category registry added by this same feature).
--
-- Why this migration exists at all: schema.sql's CREATE TABLE IF NOT EXISTS
-- for contatos_agenda was written assuming the table didn't exist yet on the
-- live database (that was true when the Agenda module's ER diagram was last
-- checked). It turned out the table HAD already been created earlier, from
-- the very first version of the module, with the old free-text "categoria"
-- column -- so CREATE TABLE IF NOT EXISTS silently left it untouched. This
-- script does the actual conversion instead.
--
-- Safe to run more than once: every step below is guarded (IF NOT EXISTS /
-- ON CONFLICT DO NOTHING / IF EXISTS), and no row is ever deleted.
BEGIN;

-- 1) Add the new column (nullable for now -- backfilled below).
ALTER TABLE contatos_agenda
    ADD COLUMN IF NOT EXISTS categoria_id INTEGER REFERENCES categorias_agenda(id);

-- 2) Make sure every distinct category name already used in the old
--    free-text column has a matching row in categorias_agenda, normalized
--    the same way ContactCategoryService does (trimmed, uppercase) -- this
--    targets the same UPPER(TRIM(nome)) unique index schema.sql creates.
INSERT INTO categorias_agenda (nome, ativo)
SELECT DISTINCT UPPER(TRIM(categoria)), TRUE
FROM contatos_agenda
WHERE categoria IS NOT NULL AND TRIM(categoria) <> ''
ON CONFLICT (UPPER(TRIM(nome))) DO NOTHING;

-- 3) Point each contact at its matching category row.
UPDATE contatos_agenda c
SET categoria_id = cat.id
FROM categorias_agenda cat
WHERE c.categoria_id IS NULL
  AND c.categoria IS NOT NULL
  AND TRIM(c.categoria) <> ''
  AND UPPER(TRIM(cat.nome)) = UPPER(TRIM(c.categoria));

-- 4) The free-text column is now superseded by categoria_id (its values were
--    migrated in step 3) -- drop it so the table matches schema.sql, which
--    no longer declares it either.
ALTER TABLE contatos_agenda DROP COLUMN IF EXISTS categoria;

-- 5) Same index schema.sql creates for a fresh install.
CREATE INDEX IF NOT EXISTS idx_contatos_agenda_categoria ON contatos_agenda (categoria_id);

COMMIT;

-- Verify afterwards:
-- SELECT c.id, c.nome, c.categoria_id, cat.nome AS categoria_nome
-- FROM contatos_agenda c LEFT JOIN categorias_agenda cat ON cat.id = c.categoria_id
-- ORDER BY c.nome;
