-- =====================================================================
-- Harmonia - Schema do banco de dados principal (PostgreSQL)
-- Banco: sysgest_hospital
--
-- Como aplicar:
--   1. Crie o banco:  CREATE DATABASE sysgest_hospital;
--   2. Rode este arquivo:  psql -U postgres -d sysgest_hospital -f schema.sql
--
-- Este script cria as tabelas dos 3 modulos + tabela de usuarios/login e
-- ja insere os 3 usuarios iniciais (RH, Financeiro, CIAU) com senhas de
-- EXEMPLO que devem ser trocadas assim que possivel.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Extensao para gerar UUID caso queira usar identificadores nao sequenciais
-- (opcional; o schema abaixo usa SERIAL/BIGSERIAL por simplicidade)
-- ---------------------------------------------------------------------
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- MODULO: LOGIN / USUARIOS
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id                    SERIAL PRIMARY KEY,
    nome_usuario          VARCHAR(50)  NOT NULL UNIQUE,
    senha_hash            VARCHAR(100) NOT NULL,           -- hash BCrypt, nunca texto puro
    perfil                VARCHAR(20)  NOT NULL CHECK (perfil IN ('RH', 'FINANCEIRO', 'CIAU')),
    nome_exibicao         VARCHAR(100) NOT NULL,
    caminho_foto_perfil   VARCHAR(255),
    ativo                 BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em             TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- MODULO: REFEITORIO
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS colaboradores (
    id                     SERIAL PRIMARY KEY,
    nome                   VARCHAR(150) NOT NULL,
    cpf                    VARCHAR(14)  NOT NULL UNIQUE,
    empresa_terceirizada   VARCHAR(150),
    cargo                  VARCHAR(100),
    ativo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em              TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS refeicoes (
    id               SERIAL PRIMARY KEY,
    colaborador_id   INTEGER NOT NULL REFERENCES colaboradores(id),
    data             DATE    NOT NULL,
    horario          TIME    NOT NULL,
    preco            NUMERIC(10, 2) NOT NULL CHECK (preco >= 0),
    tipo             VARCHAR(20) NOT NULL
                     CHECK (tipo IN ('CAFE_DA_MANHA', 'ALMOCO', 'LANCHE', 'JANTAR', 'CEIA')),
    ativo            BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em        TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indice para acelerar a consulta mais comum: relatorio por periodo
CREATE INDEX IF NOT EXISTS idx_refeicoes_data ON refeicoes (data);
CREATE INDEX IF NOT EXISTS idx_refeicoes_colaborador ON refeicoes (colaborador_id);

-- Valores configuraveis das refeicoes (tela Relatorios > "Valores das
-- refeicoes"). Um tipo sem linha aqui ainda usa o PLACEHOLDER de
-- TipoRefeicao.getPrecoPadrao() no codigo -- so' passa a valer o preco daqui
-- depois que alguem salvar um valor pra ele pela tela.
CREATE TABLE IF NOT EXISTS precos_refeicao (
    tipo    VARCHAR(20) PRIMARY KEY
            CHECK (tipo IN ('CAFE_DA_MANHA', 'ALMOCO', 'LANCHE', 'JANTAR', 'CEIA')),
    preco   NUMERIC(10, 2) NOT NULL CHECK (preco >= 0)
);

-- =====================================================================
-- MIGRACAO: se o banco ja existia ANTES da coluna "ativo" (inativacao de
-- refeicoes, ao inves de excluir de vez, para manter o log/auditoria
-- completo) e/ou ANTES da tabela "precos_refeicao" acima, rode os comandos
-- abaixo manualmente uma unica vez -- o CREATE TABLE IF NOT EXISTS de uma
-- tabela nova funciona normalmente num banco ja existente, mas o ALTER TABLE
-- de uma coluna nova numa tabela ja existente precisa ser manual:
--
--   ALTER TABLE refeicoes ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
--
--   CREATE TABLE IF NOT EXISTS precos_refeicao (
--       tipo    VARCHAR(20) PRIMARY KEY
--               CHECK (tipo IN ('CAFE_DA_MANHA', 'ALMOCO', 'LANCHE', 'JANTAR', 'CEIA')),
--       preco   NUMERIC(10, 2) NOT NULL CHECK (preco >= 0)
--   );
-- =====================================================================

-- ---------------------------------------------------------------------
-- MODULO: DESCARTE DE ATIVOS
-- (numero_patrimonio/descricao/etc vem originalmente do banco EXTERNO;
--  aqui guardamos uma copia no momento da emissao do laudo, para manter
--  o historico mesmo que o dado mude depois no sistema externo)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS laudos_baixa_ativos (
    id                   SERIAL PRIMARY KEY,
    numero_patrimonio    VARCHAR(50)  NOT NULL,
    descricao_ativo      VARCHAR(255),
    localizacao          VARCHAR(150),
    data_aquisicao       DATE,
    valor_aquisicao      VARCHAR(50),
    motivo_baixa         TEXT         NOT NULL,
    parecer_tecnico      TEXT,
    responsavel_tecnico  VARCHAR(150) NOT NULL,
    data_laudo           DATE         NOT NULL DEFAULT CURRENT_DATE,
    situacao             VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE'
                         CHECK (situacao IN ('PENDENTE', 'APROVADO', 'REPROVADO')),
    criado_em            TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_laudos_patrimonio ON laudos_baixa_ativos (numero_patrimonio);

-- ---------------------------------------------------------------------
-- MODULO: IMPRESSORAS
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS impressoras_manutencao (
    id                       SERIAL PRIMARY KEY,
    modelo                   VARCHAR(100) NOT NULL,
    patrimonio               VARCHAR(50),
    setor_origem             VARCHAR(100),
    assistencia_tecnica      VARCHAR(150) NOT NULL,
    data_envio               DATE         NOT NULL,
    data_retorno             DATE,                      -- NULL enquanto ainda esta em conserto
    defeito_relatado         TEXT,
    servico_realizado        TEXT,
    em_garantia              BOOLEAN      NOT NULL DEFAULT FALSE,
    caminho_anexo_garantia   VARCHAR(255),
    criado_em                TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_impressoras_data_envio ON impressoras_manutencao (data_envio);

-- =====================================================================
-- DADOS INICIAIS: 3 usuarios de exemplo
-- As senhas abaixo sao HASHES BCrypt da senha "mudar123" (apenas para o
-- primeiro acesso). TROQUE essas senhas assim que o sistema estiver no ar.
-- Para gerar um novo hash, use PasswordUtil.hash("novaSenha") em um teste
-- Java rapido, ou a classe utilitaria em util/PasswordUtil.java.
-- =====================================================================
INSERT INTO usuarios (nome_usuario, senha_hash, perfil, nome_exibicao) VALUES
    ('rh.usuario',         '$2a$12$CwTycUXWue0Thq9StjUM0uJ8Q4z9y5G0Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5', 'RH',         'Recursos Humanos'),
    ('financeiro.usuario', '$2a$12$CwTycUXWue0Thq9StjUM0uJ8Q4z9y5G0Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5', 'FINANCEIRO', 'Financeiro'),
    ('ciau.usuario',       '$2a$12$CwTycUXWue0Thq9StjUM0uJ8Q4z9y5G0Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5', 'CIAU',       'CIAU')
ON CONFLICT (nome_usuario) DO NOTHING;

-- ATENCAO: os hashes acima sao apenas ILUSTRATIVOS (formato correto do BCrypt,
-- mas nao necessariamente validos para "mudar123"). Use a classe
-- GeradorDeHashInicial (utilitario incluso, ver README) para gerar e substituir
-- por hashes reais antes do primeiro uso.
