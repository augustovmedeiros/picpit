package br.com.catalogo.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Hierarquia de exceções")
class ExcecoesTest {

    @Test
    @DisplayName("ValidacaoException guarda os erros por campo, na ordem, sem permitir alteração")
    void validacaoGuardaErros() {
        Map<String, String> erros = new LinkedHashMap<>();
        erros.put("titulo", "O título é obrigatório.");
        erros.put("nota", "A nota deve estar entre 0 e 10.");

        ValidacaoException e = new ValidacaoException(erros);
        erros.put("genero", "alterado depois");

        assertEquals(2, e.getErros().size(), "cópia defensiva");
        assertTrue(e.temErro("titulo"));
        assertFalse(e.temErro("genero"));
        assertEquals("Dados inválidos - titulo: O título é obrigatório.; nota: A nota deve estar entre 0 e 10.",
                e.getMessage());
        assertThrows(UnsupportedOperationException.class, () -> e.getErros().put("x", "y"));
    }

    @Test
    @DisplayName("ValidacaoException exige ao menos um erro")
    void validacaoSemErros() {
        assertThrows(IllegalArgumentException.class, () -> new ValidacaoException(Map.of()));
    }

    @Test
    @DisplayName("ValidacaoException é checada e as demais são não checadas")
    void checadaENaoChecadas() {
        assertFalse(RuntimeException.class.isAssignableFrom(ValidacaoException.class));
        assertTrue(Exception.class.isAssignableFrom(ValidacaoException.class));

        assertInstanceOf(RuntimeException.class, new PersistenciaException("x", null));
        assertInstanceOf(PersistenciaException.class, new RegistroDuplicadoException("x", null));
        assertInstanceOf(CatalogoException.class, new ItemNaoEncontradoException(1));
        assertInstanceOf(CatalogoException.class, new RequisicaoInvalidaException("x"));
    }

    @Test
    @DisplayName("PersistenciaException preserva a SQLException original como causa")
    void persistenciaPreservaCausa() {
        SQLException causa = new SQLException("Connection refused", "08001");
        PersistenciaException e = new PersistenciaException("Banco indisponível.", causa);

        assertSame(causa, e.getCause());
        assertEquals("Banco indisponível.", e.getMessage());
    }

    @Test
    @DisplayName("ItemNaoEncontradoException informa o id em mensagem amigável")
    void itemNaoEncontrado() {
        ItemNaoEncontradoException e = new ItemNaoEncontradoException(42);

        assertEquals(42, e.getId());
        assertEquals("O item #42 não foi encontrado. Ele pode ter sido excluído.", e.getMessage());
    }
}

