package br.com.catalogo.service;

import br.com.catalogo.dao.ItemDAO;
import br.com.catalogo.exception.ItemNaoEncontradoException;
import br.com.catalogo.exception.RegistroDuplicadoException;
import br.com.catalogo.exception.ValidacaoException;
import br.com.catalogo.model.ItemCatalogo;
import br.com.catalogo.model.TipoMidia;
import br.com.catalogo.validation.Validador;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Regras de negócio do catálogo. Fica entre o controlador (Servlet) e o DAO.
 * <p>
 * Responsabilidades no tratamento de erros:
 * <ul>
 *   <li>validar <em>sempre</em> antes de gravar (o DAO nunca recebe dados inválidos);</li>
 *   <li>impedir duplicidades (título + tipo + ano) com mensagem clara;</li>
 *   <li>converter {@link RegistroDuplicadoException} (concorrência) em {@link ValidacaoException};</li>
 *   <li>lançar {@link ItemNaoEncontradoException} para ids inexistentes.</li>
 * </ul>
 */
public class ItemService {

    private static final Logger LOG = Logger.getLogger(ItemService.class.getName());

    private final ItemDAO dao;
    private final Validador validador;

    public ItemService(ItemDAO dao, Validador validador) {
        this.dao = Objects.requireNonNull(dao, "dao");
        this.validador = Objects.requireNonNull(validador, "validador");
    }

    public Validador getValidador() {
        return validador;
    }

    public List<ItemCatalogo> listar(String termoBusca, TipoMidia tipo) {
        String termo = termoBusca == null || termoBusca.isBlank() ? null : termoBusca.trim();
        return Collections.unmodifiableList(dao.listar(termo, tipo));
    }

    public ItemCatalogo buscarPorId(int id) {
        return dao.buscarPorId(id).orElseThrow(() -> new ItemNaoEncontradoException(id));
    }

    /**
     * Converte os campos do formulário, valida e grava (inclusão ou alteração).
     *
     * @throws ValidacaoException se algum campo for inválido ou o item já existir
     */
    public ItemCatalogo salvarFormulario(Map<String, String> campos) throws ValidacaoException {
        return salvar(validador.converter(campos));
    }

    /**
     * Valida e grava o item: insere se não tiver id, atualiza caso contrário.
     *
     * @throws ValidacaoException          dados inválidos ou duplicados
     * @throws ItemNaoEncontradoException  ao atualizar um id inexistente
     */
    public ItemCatalogo salvar(ItemCatalogo item) throws ValidacaoException {
        normalizar(item);
        validador.validar(item);

        if (dao.existeDuplicado(item.getTitulo(), item.getTipo(), item.getAnoLancamento(), item.getId())) {
            throw new ValidacaoException("titulo", mensagemDuplicado(item));
        }

        try {
            if (item.isNovo()) {
                ItemCatalogo inserido = dao.inserir(item);
                LOG.info(() -> "Item cadastrado: " + inserido);
                return inserido;
            }
            if (!dao.atualizar(item)) {
                throw new ItemNaoEncontradoException(item.getId());
            }
            LOG.info(() -> "Item atualizado: " + item);
            return item;
        } catch (RegistroDuplicadoException e) {
            // Outro usuário gravou o mesmo item entre a verificação e o INSERT/UPDATE.
            throw new ValidacaoException("titulo", mensagemDuplicado(item));
        }
    }

    public void excluir(int id) {
        if (!dao.excluir(id)) {
            throw new ItemNaoEncontradoException(id);
        }
        LOG.info(() -> "Item excluído: #" + id);
    }

    /** Total por tipo, incluindo os tipos sem nenhum item (valor zero). */
    public Map<TipoMidia, Integer> resumoPorTipo() {
        Map<TipoMidia, Integer> resumo = new EnumMap<>(TipoMidia.class);
        for (TipoMidia tipo : TipoMidia.values()) {
            resumo.put(tipo, 0);
        }
        resumo.putAll(dao.contarPorTipo());
        return resumo;
    }

    private static String mensagemDuplicado(ItemCatalogo item) {
        return String.format("Já existe um(a) %s com o título \"%s\" lançado(a) em %d.",
                item.getTipo().getDescricao().toLowerCase(), item.getTitulo(), item.getAnoLancamento());
    }

    /** Remove espaços extras e transforma textos opcionais vazios em {@code null}. */
    private static void normalizar(ItemCatalogo item) {
        if (item == null) {
            return;
        }
        item.setTitulo(limpar(item.getTitulo()));
        item.setGenero(limpar(item.getGenero()));
        item.setCriador(limpar(item.getCriador()));
        item.setSinopse(limpar(item.getSinopse()));
    }

    private static String limpar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim().replaceAll("[ \\t]{2,}", " ");
        return limpo.isEmpty() ? null : limpo;
    }
}
