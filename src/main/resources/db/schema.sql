-- =====================================================================
-- Harmonia - Schema do banco de dados principal (PostgreSQL)
-- Banco: sysgest_hospital
--
-- Como aplicar:
--   1. Crie o banco:  CREATE DATABASE sysgest_hospital;
--   2. Rode este arquivo:  psql -U postgres -d sysgest_hospital -f schema.sql
--
-- Este script cria as tabelas dos 3 módulos + tabela de usuários/login e
-- já insere os 3 usuários iniciais (RH, Financeiro, CIAU) com senhas de
-- EXEMPLO que devem ser trocadas assim que possível.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Extensão para gerar UUID caso queira usar identificadores não sequenciais
-- (opcional; o schema abaixo usa SERIAL/BIGSERIAL por simplicidade)
-- ---------------------------------------------------------------------
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- MÓDULO: LOGIN / USUÁRIOS
-- ---------------------------------------------------------------------
-- perfil (RH/FINANCEIRO/CIAU) is now just a label -- actual module access is
-- the acesso_* columns below, editable per account on the Gerenciar
-- Usuários screen (see Role.java's class comment for why this changed).
-- perfil still exclusively gates that screen itself (CIAU only).
CREATE TABLE IF NOT EXISTS usuarios (
    id                            SERIAL PRIMARY KEY,
    nome_usuario                  VARCHAR(50)  NOT NULL UNIQUE,
    senha_hash                    VARCHAR(100) NOT NULL,           -- hash BCrypt, nunca texto puro
    perfil                        VARCHAR(20)  NOT NULL CHECK (perfil IN ('RH', 'FINANCEIRO', 'CIAU')),
    nome_exibicao                 VARCHAR(100) NOT NULL,
    caminho_foto_perfil           VARCHAR(255),
    ativo                         BOOLEAN      NOT NULL DEFAULT TRUE,
    acesso_refeitorio             BOOLEAN      NOT NULL DEFAULT FALSE,
    acesso_agenda_telefonica      BOOLEAN      NOT NULL DEFAULT FALSE,
    acesso_assistencia_tecnica    BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em                     TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- MÓDULO: REFEITÓRIO
-- ---------------------------------------------------------------------

-- Categorias dos colaboradores (o grupo pelo qual a refeição é cobrada:
-- ALUNOS, UNICAMP, FUNCIONARIOS...). Antes isso era texto livre digitado no
-- cadastro do colaborador, o que fazia a mesma empresa virar "UNICAMP",
-- "UNICAMP LTDA" e "UNICAM" -- cada variante virando um bloco separado, com
-- subtotal próprio, no relatório usado para cobrar a terceirizada. Agora é
-- cadastro próprio, escolhido numa lista na tela.
CREATE TABLE IF NOT EXISTS categorias (
    id         SERIAL PRIMARY KEY,
    nome       VARCHAR(150) NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Unicidade que ignora caixa e espaços: impede "Alunos" e "ALUNOS" de
-- coexistirem mesmo que alguém insira direto no banco, sem passar pela tela.
-- É a mesma expressão que CategoryDaoImpl.findByName usa na busca.
CREATE UNIQUE INDEX IF NOT EXISTS uq_categorias_nome ON categorias (UPPER(TRIM(nome)));

CREATE TABLE IF NOT EXISTS colaboradores (
    id                     SERIAL PRIMARY KEY,
    nome                   VARCHAR(150) NOT NULL,
    cpf                    VARCHAR(14)  NOT NULL UNIQUE,
    -- Coluna legada: guardava a categoria como texto livre. Nada no código
    -- lê ou grava ela desde a migração para a tabela "categorias" -- fica
    -- aqui como backup dos valores anteriores e pode ser removida depois que
    -- a migração estiver validada.
    empresa_terceirizada   VARCHAR(150),
    categoria_id           INTEGER      REFERENCES categorias(id),
    cargo                  VARCHAR(100),
    -- Marca uma matrícula intencionalmente compartilhada por mais de uma
    -- pessoa (ex.: crachá de plantão médico usado por quem estiver de
    -- plantão, não vinculado a uma única pessoa). Quando TRUE, a regra de
    -- "1 refeição por tipo por dia" não se aplica a essa matrícula.
    uso_compartilhado      BOOLEAN      NOT NULL DEFAULT FALSE,
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

-- Índice para acelerar a consulta mais comum: relatório por período
CREATE INDEX IF NOT EXISTS idx_colaboradores_categoria ON colaboradores (categoria_id);

CREATE INDEX IF NOT EXISTS idx_refeicoes_data ON refeicoes (data);
CREATE INDEX IF NOT EXISTS idx_refeicoes_colaborador ON refeicoes (colaborador_id);

-- Valores configuráveis das refeições (tela Relatórios > "Valores das
-- refeições"). Um tipo sem linha aqui ainda usa o PLACEHOLDER de
-- MealType.getDefaultPrice() no código -- só passa a valer o preço daqui
-- depois que alguém salvar um valor pra ele pela tela.
CREATE TABLE IF NOT EXISTS precos_refeicao (
    tipo    VARCHAR(20) PRIMARY KEY
            CHECK (tipo IN ('CAFE_DA_MANHA', 'ALMOCO', 'LANCHE', 'JANTAR', 'CEIA')),
    preco   NUMERIC(10, 2) NOT NULL CHECK (preco >= 0)
);

-- =====================================================================
-- MIGRAÇÃO DAS CATEGORIAS: num banco que já existe, o CREATE TABLE
-- IF NOT EXISTS acima cria a tabela "categorias" vazia, mas NÃO preenche
-- nada nem liga os colaboradores nela. Para isso existe um script separado,
-- que pode ser executado como está (não é comentário):
--
--   db/migrations/2026-09-17_categorias.sql
--
-- Ele cria uma categoria para cada valor distinto já gravado em
-- empresa_terceirizada (normalizando caixa e espaços, de modo que "Alunos",
-- "ALUNOS " e "alunos" viram UMA categoria só) e aponta cada colaborador
-- para a sua. É idempotente: rodar de novo não duplica nada.
-- =====================================================================

-- =====================================================================
-- MIGRAÇÃO DAS PERMISSÕES POR MÓDULO: mesma situação -- num banco que já
-- existe, o CREATE TABLE IF NOT EXISTS de "usuarios" acima não adiciona as
-- colunas acesso_refeitorio/acesso_agenda_telefonica/acesso_assistencia_tecnica
-- a uma tabela "usuarios" já existente. Script separado, também executável
-- como está:
--
--   db/migrations/2026-09-18_permissoes_modulo.sql
--
-- Ele adiciona as 3 colunas e preenche cada conta já existente com o
-- acesso que o perfil dela já dava hoje (RH -> Refeitório, FINANCEIRO ->
-- Agenda, CIAU -> os três), então ninguém perde acesso por causa da
-- migração -- os ajustes finos (dar Agenda para alguém do RH, por
-- exemplo) ficam por conta do CIAU depois, pela tela Gerenciar Usuários.
-- =====================================================================

-- =====================================================================
-- MIGRAÇÃO: se o banco já existia ANTES da coluna "ativo" (inativação de
-- refeições, ao invés de excluir de vez, para manter o log/auditoria
-- completo) e/ou ANTES da tabela "precos_refeicao" acima, rode os comandos
-- abaixo manualmente uma única vez -- o CREATE TABLE IF NOT EXISTS de uma
-- tabela nova funciona normalmente num banco já existente, mas o ALTER TABLE
-- de uma coluna nova numa tabela já existente precisa ser manual:
--
--   ALTER TABLE refeicoes ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
--
--   CREATE TABLE IF NOT EXISTS precos_refeicao (
--       tipo    VARCHAR(20) PRIMARY KEY
--               CHECK (tipo IN ('CAFE_DA_MANHA', 'ALMOCO', 'LANCHE', 'JANTAR', 'CEIA')),
--       preco   NUMERIC(10, 2) NOT NULL CHECK (preco >= 0)
--   );
--
-- Se o banco já existia ANTES da coluna "dias_garantia" em
-- impressoras_manutencao (módulo Impressoras), rode também:
--
--   ALTER TABLE impressoras_manutencao ADD COLUMN IF NOT EXISTS dias_garantia INTEGER;
--
-- A coluna antiga "em_garantia" (BOOLEAN) NÃO é removida nem alterada por
-- esse comando -- ela só deixa de ser usada pelo app, mantendo o valor
-- padrão (FALSE) e sem quebrar nada que já exista no banco.
--
-- Se o banco já existia ANTES da coluna "ativo" em impressoras_manutencao,
-- rode também:
--
--   ALTER TABLE impressoras_manutencao ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
--
-- Se o banco já existia ANTES das colunas "marca" e "numero_serie" em
-- impressoras_manutencao, rode também:
--
--   ALTER TABLE impressoras_manutencao ADD COLUMN IF NOT EXISTS marca VARCHAR(100);
--   ALTER TABLE impressoras_manutencao ADD COLUMN IF NOT EXISTS numero_serie VARCHAR(100);
--
-- O módulo Impressoras virou "Assistência Técnica" (agora cobre qualquer
-- equipamento enviado para conserto -- impressoras, notebooks, projetores,
-- etc. -- não só impressoras). A tabela continua se chamando
-- "impressoras_manutencao" por compatibilidade (não é renomeada, só ganha
-- uma coluna nova). Se o banco já existia ANTES da coluna "tipo_equipamento",
-- rode também:
--
--   ALTER TABLE impressoras_manutencao ADD COLUMN IF NOT EXISTS tipo_equipamento VARCHAR(50);
--
-- Se o banco já existia ANTES da coluna "uso_compartilhado" em
-- colaboradores (matrículas compartilhadas, ex.: plantão médico), rode
-- também:
--
--   ALTER TABLE colaboradores ADD COLUMN IF NOT EXISTS uso_compartilhado BOOLEAN NOT NULL DEFAULT FALSE;
-- =====================================================================

-- ---------------------------------------------------------------------
-- MÓDULO: DESCARTE DE ATIVOS
-- (numero_patrimonio/descricao/etc vem originalmente do banco EXTERNO;
--  aqui guardamos uma cópia no momento da emissão do laudo, para manter
--  o histórico mesmo que o dado mude depois no sistema externo)
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
-- MÓDULO: ASSISTÊNCIA TÉCNICA
-- (registro de qualquer equipamento enviado para conserto -- impressoras,
--  notebooks, projetores, monitores, desktops/CPU, etc. A tabela mantém o
--  nome "impressoras_manutencao" por compatibilidade com o banco já em uso;
--  o app é que passou a tratá-la como genérica através da coluna
--  "tipo_equipamento" abaixo)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS impressoras_manutencao (
    id                       SERIAL PRIMARY KEY,
    tipo_equipamento         VARCHAR(50),                -- Impressora, Notebook, Projetor, Monitor, Desktop/CPU, ou texto livre ("Outro")
    modelo                   VARCHAR(100) NOT NULL,
    marca                    VARCHAR(100),
    patrimonio               VARCHAR(50),
    numero_serie             VARCHAR(100),
    setor_origem             VARCHAR(100),
    assistencia_tecnica      VARCHAR(150) NOT NULL,
    data_envio               DATE         NOT NULL,
    data_retorno             DATE,                      -- NULL enquanto ainda está em conserto
    defeito_relatado         TEXT,
    servico_realizado        TEXT,
    em_garantia              BOOLEAN      NOT NULL DEFAULT FALSE, -- não usada mais pelo app; ver dias_garantia abaixo
    dias_garantia            INTEGER,                   -- dias de garantia dados pela assistência (comum: 30 ou 60, mas varia); NULL = não informado
    caminho_anexo_garantia   VARCHAR(255),               -- caminho da nota (PDF) anexada ao registro
    ativo                    BOOLEAN      NOT NULL DEFAULT TRUE, -- inativação em vez de exclusão definitiva, para manter o histórico -- mesmo padrão de "colaboradores"/"refeicoes"
    criado_em                TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_impressoras_data_envio ON impressoras_manutencao (data_envio);

-- ---------------------------------------------------------------------
-- MÓDULO: AGENDA TELEFÔNICA
-- (substitui a planilha Excel "Agenda v2.0" que a recepção usava antes.
--  Categoria agora é cadastro próprio (tabela categorias_agenda), com tela
--  de criar/editar/inativar igual à aba Categorias do Refeitório -- SEPARADA
--  da tabela "categorias" usada pelos colaboradores: são dois conceitos
--  diferentes (grupo de cobrança de colaborador x tipo/origem de contato),
--  então compartilhar a mesma tabela misturaria as duas listas.)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categorias_agenda (
    id         SERIAL PRIMARY KEY,
    nome       VARCHAR(150) NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Mesma unicidade (ignora caixa e espaços) da tabela "categorias".
CREATE UNIQUE INDEX IF NOT EXISTS uq_categorias_agenda_nome ON categorias_agenda (UPPER(TRIM(nome)));

CREATE TABLE IF NOT EXISTS contatos_agenda (
    id             SERIAL PRIMARY KEY,
    nome           VARCHAR(150) NOT NULL,
    categoria_id   INTEGER      REFERENCES categorias_agenda(id),
    celular        VARCHAR(30),
    telefone       VARCHAR(30),
    email          VARCHAR(150),
    observacao     TEXT,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE, -- inativação em vez de exclusão definitiva, mesmo padrão das outras tabelas
    criado_em      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_contatos_agenda_categoria ON contatos_agenda (categoria_id);

-- Ambas as tabelas são novas (nenhuma coluna adicionada a tabela já
-- existente), então os CREATE TABLE IF NOT EXISTS acima já bastam num banco
-- já em uso -- não precisa de script de migração separado, igual aconteceu
-- com laudos_baixa_ativos. Os ~5.000 contatos da planilha antiga NÃO foram
-- importados para cá (ver AgendaTelefonicaController) -- o cadastro começa
-- vazio, e as categorias também: use a aba Categorias da própria tela para
-- cadastrar as que fizerem sentido (a conversa que definiu essa lista tinha
-- ~35 sugestões extraídas da planilha antiga).

-- =====================================================================
-- DADOS INICIAIS: 3 usuários de exemplo
-- As senhas abaixo são HASHES BCrypt da senha "mudar123" (apenas para o
-- primeiro acesso). TROQUE essas senhas assim que o sistema estiver no ar.
-- Para gerar um novo hash, use PasswordUtil.hash("novaSenha") em um teste
-- Java rápido, ou a classe utilitária em util/PasswordUtil.java.
-- =====================================================================
-- acesso_* preenchido aqui só para estes 3 usuários de exemplo terem, de
-- cara, o mesmo acesso que o perfil deles sempre deu -- num banco já em
-- uso, é a migração de permissões (acima) que faz esse mesmo preenchimento
-- para as contas reais já cadastradas.
INSERT INTO usuarios (nome_usuario, senha_hash, perfil, nome_exibicao,
                       acesso_refeitorio, acesso_agenda_telefonica, acesso_assistencia_tecnica) VALUES
    ('rh.usuario',         '$2a$12$CwTycUXWue0Thq9StjUM0uJ8Q4z9y5G0Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5', 'RH',         'Recursos Humanos', TRUE,  FALSE, FALSE),
    ('financeiro.usuario', '$2a$12$CwTycUXWue0Thq9StjUM0uJ8Q4z9y5G0Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5', 'FINANCEIRO', 'Financeiro',       FALSE, TRUE,  FALSE),
    ('ciau.usuario',       '$2a$12$CwTycUXWue0Thq9StjUM0uJ8Q4z9y5G0Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5', 'CIAU',       'CIAU',             TRUE,  TRUE,  TRUE)
ON CONFLICT (nome_usuario) DO NOTHING;

-- ATENÇÃO: os hashes acima são apenas ILUSTRATIVOS (formato correto do BCrypt,
-- mas não necessariamente válidos para "mudar123"). Use a classe
-- InitialHashGenerator (utilitário incluso, ver README) para gerar e substituir
-- por hashes reais antes do primeiro uso.
