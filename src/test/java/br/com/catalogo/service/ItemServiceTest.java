package br.com.catalogo.service;

import br.com.catalogo.exception.ItemNaoEncontradoException;
import br.com.catalogo.exception.ValidacaoException;
import br.com.catalogo.model.ItemCatalogo;
import br.com.catalogo.model.TipoMidia;
import br.com.catalogo.validation.Validador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Serviço do catálogo (regras de negócio)")
class ItemServiceTest {

    private static final Clock RELOGIO_2025 =
            Clock.fixed(Instant.parse("2025-06-15T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private ItemDAOFalso dao;
    private ItemService servico;

    @BeforeEach
    void preparar() {
        dao = new ItemDAOFalso();
        servico = new ItemService(dao, new Validador(RELOGIO_2025));
    }

    private static ItemCatalogo item(String titulo, TipoMidia tipo, int ano) {
        return new ItemCatalogo(titulo, tipo, "Drama", ano);
    }

    private static Map<String, String> formulario(String titulo, String tipo, String ano, String nota) {
        Map<String, String> campos = new HashMap<>();
        campos.put("id", "");
        campos.put("titulo", titulo);
        campos.put("tipo", tipo);
        campos.put("genero", "Drama");
        campos.put("anoLancamento", ano);
        campos.put("nota", nota);
        return campos;
    }

    @Test
    @DisplayName("cadastra item válido e devolve o id gerado")
    void cadastraValido() throws ValidacaoException {
        ItemCatalogo salvo = servico.salvar(item("Cidade de Deus", TipoMidia.FILME, 2002));

        assertNotNull(salvo.getId());
        assertEquals(1, dao.insercoes);
        assertEquals(salvo, servico.buscarPorId(salvo.getId()));
    }

    @Test
    @DisplayName("cadastra a partir do formulário (conversão + validação)")
    void cadastraFormulario() throws ValidacaoException {
        ItemCatalogo salvo = servico.salvarFormulario(formulario("Dark", "SERIE", "2017", "9,2"));

        assertEquals(TipoMidia.SERIE, salvo.getTipo());
        assertEquals(0, new BigDecimal("9.2").compareTo(salvo.getNota()));
    }

    @Test
    @DisplayName("dados inválidos nunca chegam ao DAO")
    void invalidoNaoChegaAoDao() {
        Map<String, String> invalido = formulario("", "SERIE", "2099", "11");

        ValidacaoException e = assertThrows(ValidacaoException.class, () -> servico.salvarFormulario(invalido));

        assertTrue(e.temErro("titulo"));
        assertTrue(e.temErro("anoLancamento"));
        assertTrue(e.temErro("nota"));
        assertEquals(0, dao.insercoes);
        assertEquals(0, dao.atualizacoes);
    }

    @Test
    @DisplayName("objeto inválido montado no código também é barrado (segunda barreira)")
    void objetoInvalidoBarrado() {
        ItemCatalogo invalido = item("Filme do futuro", TipoMidia.FILME, 2030);

        assertThrows(ValidacaoException.class, () -> servico.salvar(invalido));
        assertEquals(0, dao.insercoes);
    }

    @Test
    @DisplayName("rejeita duplicado (mesmo título ignorando maiúsculas, tipo e ano)")
    void rejeitaDuplicado() throws ValidacaoException {
        servico.salvar(item("Breaking Bad", TipoMidia.SERIE, 2008));

        ValidacaoException e = assertThrows(ValidacaoException.class,
                () -> servico.salvar(item("breaking bad", TipoMidia.SERIE, 2008)));

        assertTrue(e.getErro("titulo").startsWith("Já existe"));
        assertEquals(1, dao.insercoes);
    }

    @Test
    @DisplayName("mesmo título com outro tipo ou ano não é duplicado")
    void mesmoTituloOutroTipo() throws ValidacaoException {
        servico.salvar(item("Duna", TipoMidia.LIVRO, 1965));
        servico.salvar(item("Duna", TipoMidia.FILME, 2021));
        servico.salvar(item("Duna", TipoMidia.FILME, 1984));

        assertEquals(3, servico.listar(null, null).size());
    }

    @Test
    @DisplayName("converte RegistroDuplicadoException (corrida entre usuários) em ValidacaoException")
    void corridaDuplicado() {
        dao.simularCorridaDuplicado = true;

        ValidacaoException e = assertThrows(ValidacaoException.class,
                () -> servico.salvar(item("Dark", TipoMidia.SERIE, 2017)));

        assertTrue(e.temErro("titulo"));
    }

    @Test
    @DisplayName("edição pode manter o próprio título sem acusar duplicidade")
    void edicaoMantemTitulo() throws ValidacaoException {
        ItemCatalogo salvo = servico.salvar(item("Interestelar", TipoMidia.FILME, 2014));
        salvo.setNota(new BigDecimal("9.0"));

        servico.salvar(salvo);

        assertEquals(1, dao.atualizacoes);
        assertEquals(0, new BigDecimal("9").compareTo(servico.buscarPorId(salvo.getId()).getNota()));
    }

    @Test
    @DisplayName("atualizar id inexistente lança ItemNaoEncontradoException")
    void atualizarInexistente() {
        ItemCatalogo fantasma = item("Fantasma", TipoMidia.FILME, 2000);
        fantasma.setId(99);

        ItemNaoEncontradoException e = assertThrows(ItemNaoEncontradoException.class, () -> servico.salvar(fantasma));
        assertEquals(99, e.getId());
    }

    @Test
    @DisplayName("buscar e excluir id inexistente lançam ItemNaoEncontradoException")
    void buscarExcluirInexistente() {
        assertThrows(ItemNaoEncontradoException.class, () -> servico.buscarPorId(7));
        assertThrows(ItemNaoEncontradoException.class, () -> servico.excluir(7));
    }

    @Test
    @DisplayName("exclui item existente")
    void excluiExistente() throws ValidacaoException {
        ItemCatalogo salvo = servico.salvar(item("O Hobbit", TipoMidia.LIVRO, 1937));

        servico.excluir(salvo.getId());

        assertTrue(servico.listar(null, null).isEmpty());
    }

    @Test
    @DisplayName("normaliza espaços e transforma textos opcionais vazios em null")
    void normaliza() throws ValidacaoException {
        ItemCatalogo novo = item("  O   Auto da Compadecida  ", TipoMidia.FILME, 2000);
        novo.setCriador("   ");
        novo.setSinopse("");

        ItemCatalogo salvo = servico.salvar(novo);

        assertEquals("O Auto da Compadecida", salvo.getTitulo());
        assertNull(salvo.getCriador());
        assertNull(salvo.getSinopse());
    }

    @Test
    @DisplayName("resumo por tipo inclui tipos sem itens com zero")
    void resumoComZeros() throws ValidacaoException {
        servico.salvar(item("Dom Casmurro", TipoMidia.LIVRO, 1899));
        servico.salvar(item("O Hobbit", TipoMidia.LIVRO, 1937));

        Map<TipoMidia, Integer> resumo = servico.resumoPorTipo();

        assertEquals(2, resumo.get(TipoMidia.LIVRO));
        assertEquals(0, resumo.get(TipoMidia.SERIE));
        assertEquals(0, resumo.get(TipoMidia.FILME));
    }

    @Test
    @DisplayName("busca com termo em branco equivale a listar tudo")
    void buscaEmBranco() throws ValidacaoException {
        servico.salvar(item("Dark", TipoMidia.SERIE, 2017));

        List<ItemCatalogo> itens = servico.listar("   ", null);

        assertEquals(1, itens.size());
        assertThrows(UnsupportedOperationException.class, () -> itens.add(new ItemCatalogo()));
    }
}

