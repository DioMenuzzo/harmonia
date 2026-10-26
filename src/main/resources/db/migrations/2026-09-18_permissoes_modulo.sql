-- =====================================================================
-- MIGRAÇÃO 2026-09-18: acesso aos módulos vira permissão por usuário
--
-- ANTES: o acesso a cada módulo era 100% determinado pelo perfil (RH ->
--        Refeitório, FINANCEIRO -> Agenda Telefônica, CIAU -> todos).
--
-- DEPOIS: cada conta tem 3 permissões próprias (acesso_refeitorio,
--         acesso_agenda_telefonica, acesso_assistencia_tecnica),
--         editáveis uma a uma na tela Gerenciar Usuários -- um usuário
--         pode acumular módulos que não seguem mais o perfil dele (ex.:
--         um FINANCEIRO que também precisa entrar no Refeitório). O
--         perfil continua existindo só como rótulo e continua sendo o
--         único jeito de acessar a própria tela de Gerenciar Usuários
--         (CIAU) -- isso não mudou.
--
-- Este script PODE SER EXECUTADO COMO ESTÁ (não é comentário) e é
-- idempotente: rodar de novo não altera nada que já tenha sido ajustado
-- manualmente pela tela.
--
--   psql -U <usuario> -d sysgest_hospital -f 2026-09-18_permissoes_modulo.sql
-- =====================================================================

BEGIN;

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS acesso_refeitorio BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS acesso_agenda_telefonica BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS acesso_assistencia_tecnica BOOLEAN NOT NULL DEFAULT FALSE;

-- Preserva o acesso ATUAL de cada conta como o novo valor padrão --
-- ninguém perde nem ganha acesso a um módulo só por causa desta migração;
-- o CIAU ajusta manualmente pela tela a partir de agora, se quiser.
-- A condição "AND NOT acesso_x" é só para a idempotência (rodar de novo
-- não desfaz um ajuste manual que já tenha marcado FALSE algo que o
-- perfil sugeriria TRUE).
UPDATE usuarios SET acesso_refeitorio = TRUE
    WHERE perfil IN ('RH', 'CIAU') AND NOT acesso_refeitorio;

UPDATE usuarios SET acesso_agenda_telefonica = TRUE
    WHERE perfil IN ('FINANCEIRO', 'CIAU') AND NOT acesso_agenda_telefonica;

UPDATE usuarios SET acesso_assistencia_tecnica = TRUE
    WHERE perfil = 'CIAU' AND NOT acesso_assistencia_tecnica;

COMMIT;

-- =====================================================================
-- CONFERÊNCIA (rode depois; não altera nada)
--
--      SELECT nome_usuario, perfil, acesso_refeitorio,
--             acesso_agenda_telefonica, acesso_assistencia_tecnica
--      FROM usuarios
--      ORDER BY nome_usuario;
-- =====================================================================
