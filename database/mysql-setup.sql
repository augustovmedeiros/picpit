-- =====================================================================
-- Preparação do MySQL 8 para o Catálogo (executar uma vez como root):
--
--   mysql -u root -p < database/mysql-setup.sql
--
-- Cria o banco, o usuário da aplicação (com privilégios mínimos) e a tabela.
-- Troque a senha abaixo e informe a mesma em db.properties ou na variável
-- de ambiente CATALOGO_DB_SENHA.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS catalogo
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'catalogo_app'@'localhost' IDENTIFIED BY 'catalogo123';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE ON catalogo.* TO 'catalogo_app'@'localhost';
FLUSH PRIVILEGES;

USE catalogo;

-- Mesma estrutura de src/main/resources/db/schema.sql.
-- A collation utf8mb4_0900_ai_ci torna a chave UNIQUE insensível a maiúsculas
-- e acentos ("Dark" e "dark" são considerados o mesmo título).
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
) ENGINE = InnoDB;

-- Dados de exemplo (opcional):
-- SOURCE src/main/resources/db/dados-exemplo.sql

