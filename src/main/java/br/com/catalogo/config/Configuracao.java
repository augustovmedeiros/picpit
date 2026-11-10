package br.com.catalogo.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Properties;

/**
 * Lê as configurações da aplicação com a seguinte prioridade:
 * <ol>
 *   <li>Propriedade de sistema da JVM: {@code -Dcatalogo.db.url=...}</li>
 *   <li>Variável de ambiente: {@code CATALOGO_DB_URL=...}</li>
 *   <li>Arquivo {@code db.properties} no classpath</li>
 * </ol>
 * Assim, credenciais reais não precisam ficar gravadas no código-fonte.
 */
public final class Configuracao {

    private static final String ARQUIVO = "db.properties";

    private final Properties arquivo;

    private Configuracao(Properties arquivo) {
        this.arquivo = arquivo;
    }

    public static Configuracao carregar() {
        Properties props = new Properties();
        try (InputStream in = Configuracao.class.getClassLoader().getResourceAsStream(ARQUIVO)) {
            if (in != null) {
                props.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + ARQUIVO, e);
        }
        return new Configuracao(props);
    }

    /** @return o valor da chave (ex.: "db.url") ou {@code null} se não estiver definido */
    public String get(String chave) {
        String valor = System.getProperty("catalogo." + chave);
        if (vazio(valor)) {
            valor = System.getenv("CATALOGO_" + chave.toUpperCase(Locale.ROOT).replace('.', '_'));
        }
        if (vazio(valor)) {
            valor = arquivo.getProperty(chave);
        }
        return vazio(valor) ? null : valor.trim();
    }

    public String obrigatorio(String chave) {
        String valor = get(chave);
        if (valor == null) {
            throw new IllegalStateException("Configuração obrigatória ausente: " + chave);
        }
        return valor;
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
