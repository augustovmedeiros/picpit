package br.com.catalogo.service;

import br.com.catalogo.dao.ItemDAO;
import br.com.catalogo.exception.RegistroDuplicadoException;
import br.com.catalogo.model.ItemCatalogo;
import br.com.catalogo.model.TipoMidia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * DAO em memória para testar a camada de serviço sem banco de dados.
 * Conta as gravações para provar que dados inválidos nunca chegam ao DAO.
 */
class ItemDAOFalso implements ItemDAO {

    final Map<Integer, ItemCatalogo> itens = new LinkedHashMap<>();
    int proximoId = 1;
    int insercoes;
    int atualizacoes;
    /** Simula outro usuário gravando o mesmo item entre a verificação e o INSERT. */
    boolean simularCorridaDuplicado;

    @Override
    public ItemCatalogo inserir(ItemCatalogo item) {
        insercoes++;
        if (simularCorridaDuplicado) {
            throw new RegistroDuplicadoException("duplicado", new java.sql.SQLException("Duplicate entry", "23000", 1062));
        }
        item.setId(proximoId++);
        itens.put(item.getId(), item);
        return item;
    }

    @Override
    public boolean atualizar(ItemCatalogo item) {
        atualizacoes++;
        if (!itens.containsKey(item.getId())) {
            return false;
        }
        itens.put(item.getId(), item);
        return true;
    }

    @Override
    public boolean excluir(int id) {
        return itens.remove(id) != null;
    }

    @Override
    public Optional<ItemCatalogo> buscarPorId(int id) {
        return Optional.ofNullable(itens.get(id));
    }

    @Override
    public List<ItemCatalogo> listar(String termoBusca, TipoMidia tipo) {
        List<ItemCatalogo> resultado = new ArrayList<>();
        for (ItemCatalogo item : itens.values()) {
            boolean termoOk = termoBusca == null
                    || item.getTitulo().toLowerCase(Locale.ROOT).contains(termoBusca.toLowerCase(Locale.ROOT));
            boolean tipoOk = tipo == null || item.getTipo() == tipo;
            if (termoOk && tipoOk) {
                resultado.add(item);
            }
        }
        resultado.sort(Comparator.comparing(ItemCatalogo::getTitulo));
        return resultado;
    }

    @Override
    public boolean existeDuplicado(String titulo, TipoMidia tipo, int anoLancamento, Integer idIgnorado) {
        return itens.values().stream().anyMatch(i -> i.getTitulo().equalsIgnoreCase(titulo)
                && i.getTipo() == tipo
                && i.getAnoLancamento() == anoLancamento
                && !i.getId().equals(idIgnorado));
    }

    @Override
    public Map<TipoMidia, Integer> contarPorTipo() {
        Map<TipoMidia, Integer> totais = new EnumMap<>(TipoMidia.class);
        itens.values().forEach(i -> totais.merge(i.getTipo(), 1, Integer::sum));
        return totais;
    }
}

