-- =====================================================================
-- REVERSÃO 2026-10-22: remove o módulo Mapa de TI
--
-- O módulo Mapa de TI (planta clicável + cadastro de equipamentos por
-- setor, ver 2026-10-19_mapa_ti.sql) foi descontinuado -- a ideia não
-- funcionou como esperado e vai ser substituída por outra coisa no lugar
-- dessa tela. Este script desfaz exatamente o que aquela migração fez:
--
--   * DROP de equipamentos_ti (antes de setores_ti, por causa da FK
--     equipamentos_ti.setor_id -> setores_ti.id)
--   * DROP de setores_ti
--   * remove a coluna usuarios.acesso_mapa_ti
--
-- Nenhuma outra tabela/coluna é afetada -- os módulos Refeitório, Agenda
-- Telefônica e Assistência Técnica continuam intactos.
--
-- É seguro executar mesmo que esta migração nunca tenha sido aplicada
-- (todos os comandos usam IF EXISTS, então rodar num banco que já estava
-- revertido, ou que nunca chegou a rodar 2026-10-19_mapa_ti.sql, não dá
-- erro -- só não faz nada).
--
--   psql -h <host> -U <usuario> -d sysgest_hospital -f 2026-10-22_revert_mapa_ti.sql
-- =====================================================================

BEGIN;

DROP TABLE IF EXISTS equipamentos_ti;
DROP TABLE IF EXISTS setores_ti;

ALTER TABLE usuarios DROP COLUMN IF EXISTS acesso_mapa_ti;

COMMIT;

-- =====================================================================
-- CONFERÊNCIA (rode depois; não altera nada)
--
--      SELECT to_regclass('setores_ti'), to_regclass('equipamentos_ti'); -- devem vir NULL
--      \d usuarios  -- não deve mais ter a coluna acesso_mapa_ti
-- =====================================================================
