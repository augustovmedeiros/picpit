package br.com.catalogo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ScriptSqlRunner")
class ScriptSqlRunnerTest {

    @Test
    @DisplayName("separa comandos por ponto e vírgula e ignora comentários")
    void separaComandos() {
        String script = """
                -- comentário
                CREATE TABLE a (id INT);
                  -- outro comentário
                INSERT INTO a VALUES (1);

                """;

        assertEquals(List.of("CREATE TABLE a (id INT)", "INSERT INTO a VALUES (1)"),
                ScriptSqlRunner.separarComandos(script));
    }

    @Test
    @DisplayName("script vazio não gera comandos")
    void scriptVazio() {
        assertTrue(ScriptSqlRunner.separarComandos("-- só comentário\n\n").isEmpty());
    }

    @Test
    @DisplayName("os scripts da aplicação existem e são lidos em UTF-8")
    void scriptsDaAplicacao() throws Exception {
        List<String> schema = ScriptSqlRunner.separarComandos(ScriptSqlRunner.lerRecurso("db/schema.sql"));
        List<String> dados = ScriptSqlRunner.separarComandos(ScriptSqlRunner.lerRecurso("db/dados-exemplo.sql"));

        assertFalse(schema.isEmpty());
        assertTrue(schema.get(0).startsWith("CREATE TABLE IF NOT EXISTS item_catalogo"));
        assertEquals(1, dados.size(), "dados de exemplo em um único INSERT");
        assertTrue(dados.get(0).contains("Grande Sertão: Veredas"));
    }

    @Test
    @DisplayName("recurso inexistente gera FileNotFoundException clara")
    void recursoInexistente() {
        FileNotFoundException e = assertThrows(FileNotFoundException.class,
                () -> ScriptSqlRunner.lerRecurso("db/nao-existe.sql"));
        assertTrue(e.getMessage().contains("db/nao-existe.sql"));
    }
}

