-- =====================================================================
-- MIGRAÇÃO 2026-10-19: módulo Mapa de TI
--
-- Novo módulo: localização dos equipamentos de TI do hospital, usando a
-- planta do prédio (src/main/resources/images/planta-hospital.png) como um
-- menu interativo -- clicar numa área da planta mostra/cadastra os
-- equipamentos daquele setor. Cria:
--
--   * usuarios.acesso_mapa_ti       -- nova permissão por conta, mesmo
--                                       padrão de acesso_refeitorio/
--                                       acesso_agenda_telefonica/
--                                       acesso_assistencia_tecnica (ver
--                                       2026-09-18_permissoes_modulo.sql)
--   * setores_ti                    -- as 8 áreas clicáveis da planta
--   * equipamentos_ti               -- os equipamentos em si, cada um
--                                       apontando para um setor
--
-- Registro SEPARADO de impressoras_manutencao/"Assistência Técnica" --
-- aquela tabela é sobre equipamento que foi mandado para conserto (data de
-- envio/retorno, garantia), esta é sobre onde o equipamento fica
-- permanentemente -- ver o Javadoc de ItEquipment.
--
-- Este script PODE SER EXECUTADO COMO ESTÁ (não é comentário) e é
-- idempotente: rodar de novo não duplica nada nem desfaz um ajuste manual
-- já feito pela tela (ADD COLUMN/CREATE TABLE/CREATE INDEX usam IF NOT
-- EXISTS, a inserção dos 8 setores usa ON CONFLICT DO NOTHING).
--
--   psql -U <usuario> -d sysgest_hospital -f 2026-10-19_mapa_ti.sql
-- =====================================================================

BEGIN;

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS acesso_mapa_ti BOOLEAN NOT NULL DEFAULT FALSE;

-- Mesmo critério da migração de permissões anterior: só CIAU ganha acesso
-- automático ao módulo novo (RH/FINANCEIRO nunca tiveram "todos os
-- módulos" no mapeamento original do perfil) -- ninguém mais perde ou
-- ganha acesso por causa desta migração; o CIAU ajusta manualmente pela
-- tela Gerenciar Usuários a partir de agora, se quiser dar acesso a mais
-- alguém. "AND NOT acesso_mapa_ti" é só para a idempotência.
UPDATE usuarios SET acesso_mapa_ti = TRUE
    WHERE perfil = 'CIAU' AND NOT acesso_mapa_ti;

CREATE TABLE IF NOT EXISTS setores_ti (
    id         SERIAL PRIMARY KEY,
    nome       VARCHAR(150) NOT NULL,
    pos_x      NUMERIC(6,4) NOT NULL,
    pos_y      NUMERIC(6,4) NOT NULL,
    largura    NUMERIC(6,4) NOT NULL,
    altura     NUMERIC(6,4) NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_setores_ti_nome ON setores_ti (UPPER(TRIM(nome)));

CREATE TABLE IF NOT EXISTS equipamentos_ti (
    id                    SERIAL PRIMARY KEY,
    setor_id              INTEGER      REFERENCES setores_ti(id),
    tipo_equipamento      VARCHAR(50),
    modelo                VARCHAR(100),
    marca                 VARCHAR(100),
    patrimonio            VARCHAR(50),
    numero_serie          VARCHAR(100),
    localizacao_detalhe   VARCHAR(200),
    responsavel           VARCHAR(150),
    observacao            TEXT,
    ativo                 BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em             TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_equipamentos_ti_setor ON equipamentos_ti (setor_id);

-- As 8 zonas foram estimadas visualmente a partir da planta (que não tem
-- nenhuma legenda de sala) -- os nomes abaixo são só posição; use a aba
-- Setores do próprio módulo para renomeá-las com o nome real de cada
-- ala/pavilhão assim que isso for decidido. Posição/tamanho são fração
-- (0 a 1) da largura/altura da planta, não pixels -- ver Sector.java.
INSERT INTO setores_ti (nome, pos_x, pos_y, largura, altura) VALUES
    ('Ala Superior Esquerda',      0.1154, 0.1273, 0.1783, 0.1455),
    ('Ala Superior Direita',       0.3916, 0.1273, 0.1818, 0.1455),
    ('Ala Inferior Esquerda',      0.1538, 0.3136, 0.1399, 0.1091),
    ('Ala Inferior Direita',       0.3916, 0.3136, 0.1608, 0.1091),
    ('Núcleo Central',             0.2937, 0.0864, 0.1014, 0.3682),
    ('Bloco Lateral',              0.5804, 0.0545, 0.0909, 0.3455),
    ('Pavilhão Hexagonal Direito', 0.3881, 0.4591, 0.1434, 0.1773),
    ('Pavilhão Hexagonal Esquerdo',0.1154, 0.4591, 0.2972, 0.4091)
ON CONFLICT (UPPER(TRIM(nome))) DO NOTHING;

COMMIT;

-- =====================================================================
-- CONFERÊNCIA (rode depois; não altera nada)
--
--      SELECT nome_usuario, perfil, acesso_mapa_ti FROM usuarios ORDER BY nome_usuario;
--      SELECT id, nome, ativo FROM setores_ti ORDER BY id;
-- =====================================================================
