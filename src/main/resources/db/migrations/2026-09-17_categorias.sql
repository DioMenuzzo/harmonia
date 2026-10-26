-- =====================================================================
-- MIGRAÇÃO 2026-09-17: categorias viram cadastro próprio
--
-- ANTES: a categoria do colaborador era texto livre digitado na tela
--        (coluna colaboradores.empresa_terceirizada). A mesma empresa
--        acabava gravada como "UNICAMP", "UNICAMP LTDA" e "UNICAM", e o
--        relatório do refeitório -- usado para cobrar a terceirizada --
--        quebrava cada variante num bloco separado, com subtotal próprio.
--
-- DEPOIS: existe a tabela "categorias" e o colaborador aponta para ela.
--         A tela oferece uma lista em vez de um campo de texto.
--
-- Este script PODE SER EXECUTADO COMO ESTÁ (não é comentário) e é
-- idempotente: rodar de novo não duplica categoria nem revincula ninguém.
--
--   psql -U <usuario> -d <banco> -f 2026-09-17_categorias.sql
--
-- A coluna antiga empresa_terceirizada é mantida intacta como backup dos
-- valores anteriores. Só remova depois de conferir o resultado (veja a
-- consulta de conferência no fim deste arquivo).
-- =====================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS categorias (
    id         SERIAL PRIMARY KEY,
    nome       VARCHAR(150) NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Unicidade ignorando caixa e espaços: é o que impede "Alunos" e "ALUNOS"
-- de voltarem a coexistir, mesmo por inserção direta no banco.
CREATE UNIQUE INDEX IF NOT EXISTS uq_categorias_nome ON categorias (UPPER(TRIM(nome)));

ALTER TABLE colaboradores ADD COLUMN IF NOT EXISTS categoria_id INTEGER REFERENCES categorias(id);
CREATE INDEX IF NOT EXISTS idx_colaboradores_categoria ON colaboradores (categoria_id);

-- Cria uma categoria para cada valor distinto já existente, normalizando
-- caixa e espaços -- é aqui que as variantes de digitação se fundem.
INSERT INTO categorias (nome)
SELECT DISTINCT UPPER(TRIM(empresa_terceirizada))
FROM colaboradores
WHERE empresa_terceirizada IS NOT NULL
  AND TRIM(empresa_terceirizada) <> ''
ON CONFLICT DO NOTHING;

-- Liga cada colaborador à sua categoria. Quem está sem categoria (NULL,
-- vazio ou só espaços) continua sem categoria -- nenhuma categoria em
-- branco é criada.
UPDATE colaboradores c
SET categoria_id = cat.id
FROM categorias cat
WHERE cat.nome = UPPER(TRIM(c.empresa_terceirizada))
  AND c.categoria_id IS NULL;

COMMIT;

-- =====================================================================
-- CONFERÊNCIA (rode depois; não altera nada)
--
-- 1) Categorias criadas e quantos colaboradores ficaram em cada uma:
--
--      SELECT cat.nome, count(c.id) AS colaboradores
--      FROM categorias cat
--      LEFT JOIN colaboradores c ON c.categoria_id = cat.id
--      GROUP BY cat.nome
--      ORDER BY cat.nome;
--
-- 2) Alguém ficou para trás? (tinha texto na coluna antiga mas não foi
--    vinculado -- o esperado é NENHUMA linha)
--
--      SELECT id, nome, empresa_terceirizada
--      FROM colaboradores
--      WHERE categoria_id IS NULL
--        AND empresa_terceirizada IS NOT NULL
--        AND TRIM(empresa_terceirizada) <> '';
--
-- 3) Variantes de ESCRITA que a migração não tem como adivinhar (ex.:
--    "UNICAMP" e "UNICAM" são categorias diferentes para o banco). Revise
--    esta lista e, na aba Categorias, renomeie/inative o que for duplicata
--    -- os colaboradores da categoria errada precisam ser reapontados pela
--    tela antes de inativá-la:
--
--      SELECT nome FROM categorias ORDER BY nome;
-- =====================================================================
