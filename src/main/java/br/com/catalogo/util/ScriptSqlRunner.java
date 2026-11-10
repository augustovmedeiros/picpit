package br.com.catalogo.util;

import br.com.catalogo.dao.ConnectionProvider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Executa scripts SQL do classpath (ex.: {@code db/schema.sql}).
 * <p>
 * Usado no modo demonstração (H2) e nos testes de integração. Os comandos são
 * separados por ";" e linhas iniciadas por "--" são ignoradas.
 */
public final class ScriptSqlRunner {

    private ScriptSqlRunner() {
    }

    public static void executar(ConnectionProvider conexoes, String recurso) throws IOException, SQLException {
        List<String> comandos = separarComandos(lerRecurso(recurso));
        try (Connection con = conexoes.getConnection(); Statement st = con.createStatement()) {
            for (String comando : comandos) {
                st.execute(comando);
            }
        }
    }

    static String lerRecurso(String recurso) throws IOException {
        try (InputStream in = ScriptSqlRunner.class.getClassLoader().getResourceAsStream(recurso)) {
            if (in == null) {
                throw new FileNotFoundException("Script não encontrado no classpath: " + recurso);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static List<String> separarComandos(String script) {
        StringBuilder semComentarios = new StringBuilder();
        for (String linha : script.split("\\R")) {
            if (!linha.trim().startsWith("--")) {
                semComentarios.append(linha).append('\n');
            }
        }
        List<String> comandos = new ArrayList<>();
        for (String comando : semComentarios.toString().split(";")) {
            if (!comando.isBlank()) {
                comandos.add(comando.trim());
            }
        }
        return comandos;
    }
}
