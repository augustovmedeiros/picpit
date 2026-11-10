package br.com.catalogo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Modelo: TipoMidia e ItemCatalogo")
class ModeloTest {

    @Test
    @DisplayName("deTexto converte ignorando maiúsculas e espaços")
    void deTextoValido() {
        assertEquals(Optional.of(TipoMidia.LIVRO), TipoMidia.deTexto("livro"));
        assertEquals(Optional.of(TipoMidia.SERIE), TipoMidia.deTexto(" Serie "));
        assertEquals(Optional.of(TipoMidia.FILME), TipoMidia.deTexto("FILME"));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "revista", "série", "1"})
    @DisplayName("deTexto devolve vazio para valores inválidos, sem lançar exceção")
    void deTextoInvalido(String valor) {
        assertTrue(TipoMidia.deTexto(valor).isEmpty());
    }

    @Test
    @DisplayName("rótulo do criador depende do tipo")
    void rotuloCriador() {
        assertEquals("Autor(a)", TipoMidia.LIVRO.getRotuloCriador());
        assertEquals("Criador(a)", TipoMidia.SERIE.getRotuloCriador());
        assertEquals("Diretor(a)", TipoMidia.FILME.getRotuloCriador());
        assertEquals("SERIE", TipoMidia.SERIE.getCodigo());
    }

    @Test
    @DisplayName("igualdade de itens é baseada no id")
    void igualdadePorId() {
        ItemCatalogo a = new ItemCatalogo("A", TipoMidia.LIVRO, "X", 2000);
        ItemCatalogo b = new ItemCatalogo("B", TipoMidia.FILME, "Y", 2001);
        assertNotEquals(a, b, "itens novos (sem id) não são iguais");

        a.setId(1);
        b.setId(1);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(new ItemCatalogo().isNovo());
    }

    @Test
    @DisplayName("data de cadastro é formatada no padrão brasileiro")
    void dataFormatada() {
        ItemCatalogo item = new ItemCatalogo();
        assertEquals("", item.getDataCadastroFormatada());

        item.setDataCadastro(LocalDateTime.of(2025, 3, 7, 9, 5));
        assertEquals("07/03/2025 09:05", item.getDataCadastroFormatada());
    }
}

