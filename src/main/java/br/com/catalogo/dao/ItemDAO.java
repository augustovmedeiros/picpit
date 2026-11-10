package br.com.catalogo.dao;

import br.com.catalogo.model.ItemCatalogo;
import br.com.catalogo.model.TipoMidia;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrato de persistência dos itens do catálogo (CRUD + consultas auxiliares).
 * <p>
 * Implementações devem lançar {@link br.com.catalogo.exception.PersistenciaException}
 * (ou {@link br.com.catalogo.exception.RegistroDuplicadoException}) em vez de
 * {@link java.sql.SQLException}, isolando as camadas superiores do JDBC.
 */
public interface ItemDAO {

    /** Insere o item e preenche o id gerado. */
    ItemCatalogo inserir(ItemCatalogo item);

    /** @return {@code false} se nenhum registro com o id do item existir */
    boolean atualizar(ItemCatalogo item);

    /** @return {@code false} se nenhum registro com o id existir */
    boolean excluir(int id);

    Optional<ItemCatalogo> buscarPorId(int id);

    /**
     * Lista os itens ordenados por título.
     *
     * @param termoBusca texto procurado em título, gênero ou criador ({@code null} = todos)
     * @param tipo       filtro por tipo ({@code null} = todos)
     */
    List<ItemCatalogo> listar(String termoBusca, TipoMidia tipo);

    /**
     * Verifica se já existe outro item com mesmo título (ignorando maiúsculas), tipo e ano.
     *
     * @param idIgnorado id do próprio item, em caso de edição ({@code null} na inclusão)
     */
    boolean existeDuplicado(String titulo, TipoMidia tipo, int anoLancamento, Integer idIgnorado);

    /** Quantidade de itens cadastrados por tipo. */
    Map<TipoMidia, Integer> contarPorTipo();
}
