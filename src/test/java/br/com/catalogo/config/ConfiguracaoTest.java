package br.com.catalogo.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Configuracao")
class ConfiguracaoTest {

    @AfterEach
    void limpar() {
        System.clearProperty("catalogo.db.usuario");
    }

    @Test
    @DisplayName("lê valores do db.properties")
    void leArquivo() {
        Configuracao cfg = Configuracao.carregar();

        assertTrue(cfg.obrigatorio("db.url").startsWith("jdbc:mysql://"));
        assertEquals("db/schema.sql", cfg.get("db.scripts"));
    }

    @Test
    @DisplayName("propriedade de sistema tem prioridade sobre o arquivo")
    void propriedadeDeSistemaPrevalece() {
        System.setProperty("catalogo.db.usuario", "  outro_usuario  ");

        assertEquals("outro_usuario", Configuracao.carregar().get("db.usuario"));
    }

    @Test
    @DisplayName("chave ausente devolve null ou falha com mensagem clara se obrigatória")
    void chaveAusente() {
        Configuracao cfg = Configuracao.carregar();

        assertNull(cfg.get("chave.inexistente"));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> cfg.obrigatorio("chave.inexistente"));
        assertTrue(e.getMessage().contains("chave.inexistente"));
    }
}

