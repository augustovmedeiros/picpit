-- =====================================================================
-- Catálogo de Livros, Séries e Filmes - estrutura do banco de dados
-- Compatível com MySQL 8 e com H2 (modo MySQL, usado nos testes/demo).
--
-- As constraints formam a "última barreira" da validação em camadas:
-- mesmo que um dado inválido passe pelo cliente e pelo servidor, o banco
-- o rejeita (NOT NULL, CHECK e UNIQUE).
--
-- Observação: este arquivo é lido pelo ScriptSqlRunner, que separa os
-- comandos por ponto e vírgula; por isso não há esse caractere nos textos.
-- =====================================================================

CREATE TABLE IF NOT EXISTS item_catalogo (
    id              INT           NOT NULL AUTO_INCREMENT,
    titulo          VARCHAR(150)  NOT NULL,
    tipo            VARCHAR(10)   NOT NULL,
    genero          VARCHAR(60)   NOT NULL,
    ano_lancamento  INT           NOT NULL,
    criador         VARCHAR(120)  NULL,
    nota            DECIMAL(3,1)  NULL,
    sinopse         VARCHAR(1000) NULL,
    data_cadastro   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_item_catalogo PRIMARY KEY (id),
    CONSTRAINT uk_item_titulo_tipo_ano UNIQUE (titulo, tipo, ano_lancamento),
    CONSTRAINT ck_item_titulo CHECK (CHAR_LENGTH(TRIM(titulo)) >= 2),
    CONSTRAINT ck_item_tipo CHECK (tipo IN ('LIVRO', 'SERIE', 'FILME')),
    CONSTRAINT ck_item_genero CHECK (CHAR_LENGTH(TRIM(genero)) >= 1),
    CONSTRAINT ck_item_ano CHECK (ano_lancamento BETWEEN 1800 AND 2100),
    CONSTRAINT ck_item_nota CHECK (nota IS NULL OR (nota >= 0 AND nota <= 10))
);

-- O limite superior do ano (ano corrente) é verificado pelo Validador: o MySQL
-- não permite funções não determinísticas, como CURRENT_DATE, dentro de CHECK.
