package br.com.catalogo.validation;

import br.com.catalogo.exception.ValidacaoException;
import br.com.catalogo.model.ItemCatalogo;
import br.com.catalogo.model.TipoMidia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Validador de itens do catálogo")
class ValidadorTest {

    /** Relógio fixo em 2025: o ano máximo aceito passa a ser 2025, independentemente da data real. */
    private static final Clock RELOGIO_2025 =
            Clock.fixed(Instant.parse("2025-06-15T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private Validador validador;
    private Map<String, String> campos;

    @BeforeEach
    void preparar() {
        validador = new Validador(RELOGIO_2025);
        campos = new HashMap<>();
        campos.put("titulo", "Dom Casmurro");
        campos.put("tipo", "LIVRO");
        campos.put("genero", "Romance");
        campos.put("anoLancamento", "1899");
        campos.put("criador", "Machado de Assis");
        campos.put("nota", "9.5");
        campos.put("sinopse", "Bentinho e Capitu.");
    }

    private ValidacaoException converterComErro() {
        return assertThrows(ValidacaoException.class, () -> validador.converter(campos));
    }

    @Nested
    @DisplayName("Entradas válidas")
    class Validas {

        @Test
        @DisplayName("converte um formulário completo")
        void formularioCompleto() throws ValidacaoException {
            ItemCatalogo item = validador.converter(campos);

            assertNull(item.getId());
            assertEquals("Dom Casmurro", item.getTitulo());
            assertEquals(TipoMidia.LIVRO, item.getTipo());
            assertEquals("Romance", item.getGenero());
            assertEquals(1899, item.getAnoLancamento());
            assertEquals("Machado de Assis", item.getCriador());
            assertEquals(0, new BigDecimal("9.5").compareTo(item.getNota()));
        }

        @Test
        @DisplayName("aceita apenas os campos obrigatórios")
        void somenteObrigatorios() throws ValidacaoException {
            campos.remove("criador");
            campos.put("nota", "");
            campos.put("sinopse", "   ");

            ItemCatalogo item = validador.converter(campos);

            assertNull(item.getCriador());
            assertNull(item.getNota());
            assertNull(item.getSinopse());
        }

        @Test
        @DisplayName("aceita nota com vírgula decimal (padrão brasileiro)")
        void notaComVirgula() throws ValidacaoException {
            campos.put("nota", "7,5");
            assertEquals(0, new BigDecimal("7.5").compareTo(validador.converter(campos).getNota()));
        }

        @ParameterizedTest(name = "nota {0}")
        @ValueSource(strings = {"0", "10", "10.0", "5.5", "0.1"})
        @DisplayName("aceita notas nos limites de 0 a 10")
        void notasNosLimites(String nota) {
            campos.put("nota", nota);
            assertDoesNotThrow(() -> validador.converter(campos));
        }

        @ParameterizedTest(name = "ano {0}")
        @ValueSource(strings = {"1800", "2000", "2025"})
        @DisplayName("aceita anos entre 1800 e o ano corrente")
        void anosNosLimites(String ano) {
            campos.put("anoLancamento", ano);
            assertDoesNotThrow(() -> validador.converter(campos));
        }

        @Test
        @DisplayName("aceita tipo em minúsculas e com espaços")
        void tipoFlexivel() throws ValidacaoException {
            campos.put("tipo", "  filme ");
            assertEquals(TipoMidia.FILME, validador.converter(campos).getTipo());
        }

        @Test
        @DisplayName("lê o id na edição")
        void idNaEdicao() throws ValidacaoException {
            campos.put("id", "42");
            assertEquals(42, validador.converter(campos).getId());
        }

        @Test
        @DisplayName("o ano máximo acompanha o relógio")
        void anoMaximoAcompanhaRelogio() {
            assertEquals(2025, validador.getAnoMaximo());
            assertEquals(1800, validador.getAnoMinimo());
        }
    }

    @Nested
    @DisplayName("Título")
    class Titulo {

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("é obrigatório (vazio ou só espaços)")
        void obrigatorio(String titulo) {
            campos.put("titulo", titulo);
            assertEquals("O título é obrigatório.", converterComErro().getErro("titulo"));
        }

        @Test
        @DisplayName("precisa de pelo menos 2 caracteres")
        void curtoDemais() {
            campos.put("titulo", "A");
            assertTrue(converterComErro().getErro("titulo").contains("pelo menos 2"));
        }

        @Test
        @DisplayName("aceita no máximo 150 caracteres")
        void longoDemais() {
            campos.put("titulo", "x".repeat(151));
            assertTrue(converterComErro().getErro("titulo").contains("no máximo 150"));
        }

        @Test
        @DisplayName("150 caracteres é o limite aceito")
        void limiteExato() {
            campos.put("titulo", "x".repeat(150));
            assertDoesNotThrow(() -> validador.converter(campos));
        }
    }

    @Nested
    @DisplayName("Tipo e gênero")
    class TipoEGenero {

        @Test
        @DisplayName("tipo é obrigatório")
        void tipoObrigatorio() {
            campos.remove("tipo");
            assertTrue(converterComErro().getErro("tipo").startsWith("Selecione o tipo"));
        }

        @ParameterizedTest(name = "tipo \"{0}\"")
        @ValueSource(strings = {"REVISTA", "123", "LIVROS"})
        @DisplayName("tipo fora da lista é rejeitado")
        void tipoInvalido(String tipo) {
            campos.put("tipo", tipo);
            assertTrue(converterComErro().getErro("tipo").startsWith("Tipo inválido"));
        }

        @Test
        @DisplayName("gênero é obrigatório")
        void generoObrigatorio() {
            campos.put("genero", " ");
            assertEquals("O gênero é obrigatório.", converterComErro().getErro("genero"));
        }

        @Test
        @DisplayName("gênero aceita no máximo 60 caracteres")
        void generoLongo() {
            campos.put("genero", "g".repeat(61));
            assertTrue(converterComErro().temErro("genero"));
        }
    }

    @Nested
    @DisplayName("Ano de lançamento")
    class AnoLancamento {

        @Test
        @DisplayName("é obrigatório")
        void obrigatorio() {
            campos.put("anoLancamento", "");
            assertEquals("O ano de lançamento é obrigatório.", converterComErro().getErro("anoLancamento"));
        }

        @Test
        @DisplayName("ano futuro é rejeitado")
        void anoFuturo() {
            campos.put("anoLancamento", "2026");
            assertEquals("O ano deve estar entre 1800 e 2025.", converterComErro().getErro("anoLancamento"));
        }

        @Test
        @DisplayName("ano anterior a 1800 é rejeitado")
        void anoAntigo() {
            campos.put("anoLancamento", "1799");
            assertTrue(converterComErro().temErro("anoLancamento"));
        }

        @ParameterizedTest(name = "ano \"{0}\"")
        @ValueSource(strings = {"abc", "20.5", "2O10", "99999999999"})
        @DisplayName("texto não numérico gera erro de conversão (sem NumberFormatException)")
        void naoNumerico(String ano) {
            campos.put("anoLancamento", ano);
            assertTrue(converterComErro().getErro("anoLancamento").startsWith("O ano deve ser um número inteiro"));
        }
    }

    @Nested
    @DisplayName("Nota")
    class Nota {

        @ParameterizedTest(name = "nota {0}")
        @ValueSource(strings = {"-0.1", "-1", "10.1", "11", "100"})
        @DisplayName("fora de 0 a 10 é rejeitada")
        void foraDoIntervalo(String nota) {
            campos.put("nota", nota);
            assertEquals("A nota deve estar entre 0 e 10.", converterComErro().getErro("nota"));
        }

        @Test
        @DisplayName("mais de uma casa decimal é rejeitada")
        void casasDecimais() {
            campos.put("nota", "7.25");
            assertTrue(converterComErro().getErro("nota").contains("uma casa decimal"));
        }

        @ParameterizedTest(name = "nota \"{0}\"")
        @ValueSource(strings = {"dez", "8,5,1", "NaN", "--1"})
        @DisplayName("texto não numérico gera erro de conversão")
        void naoNumerica(String nota) {
            campos.put("nota", nota);
            assertTrue(converterComErro().getErro("nota").startsWith("A nota deve ser um número"));
        }
    }

    @Nested
    @DisplayName("Demais regras")
    class Demais {

        @Test
        @DisplayName("criador aceita no máximo 120 caracteres")
        void criadorLongo() {
            campos.put("criador", "c".repeat(121));
            assertTrue(converterComErro().temErro("criador"));
        }

        @Test
        @DisplayName("sinopse aceita no máximo 1000 caracteres")
        void sinopseLonga() {
            campos.put("sinopse", "s".repeat(1001));
            assertTrue(converterComErro().temErro("sinopse"));
        }

        @ParameterizedTest(name = "id \"{0}\"")
        @ValueSource(strings = {"abc", "0", "-5"})
        @DisplayName("id inválido é rejeitado")
        void idInvalido(String id) {
            campos.put("id", id);
            assertTrue(converterComErro().temErro("id"));
        }

        @Test
        @DisplayName("reúne todos os erros de uma vez, na ordem dos campos")
        void acumulaErros() {
            campos.put("titulo", "");
            campos.put("tipo", "");
            campos.put("genero", "");
            campos.put("anoLancamento", "3000");
            campos.put("nota", "15");

            ValidacaoException e = converterComErro();

            assertEquals(5, e.getErros().size());
            assertEquals(java.util.List.of("titulo", "tipo", "genero", "anoLancamento", "nota"),
                    java.util.List.copyOf(e.getErros().keySet()));
        }

        @Test
        @DisplayName("erro de conversão não é sobrescrito pelo de regra")
        void conversaoTemPrioridade() {
            campos.put("anoLancamento", "abc");
            ValidacaoException e = converterComErro();
            assertTrue(e.getErro("anoLancamento").contains("número inteiro"));
        }

        @Test
        @DisplayName("validar(null) lança ValidacaoException, não NullPointerException")
        void itemNulo() {
            ValidacaoException e = assertThrows(ValidacaoException.class, () -> validador.validar(null));
            assertTrue(e.temErro("item"));
        }

        @Test
        @DisplayName("validar() também barra objetos montados no código (segunda barreira)")
        void validarObjeto() {
            ItemCatalogo item = new ItemCatalogo("Duna", TipoMidia.LIVRO, "Ficção", 1965);
            item.setNota(new BigDecimal("10.5"));

            ValidacaoException e = assertThrows(ValidacaoException.class, () -> validador.validar(item));
            assertTrue(e.temErro("nota"));
            assertFalse(e.temErro("titulo"));
        }
    }
}

