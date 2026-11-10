package br.com.catalogo.dao;

import br.com.catalogo.exception.PersistenciaException;
import br.com.catalogo.exception.RegistroDuplicadoException;
import br.com.catalogo.model.ItemCatalogo;
import br.com.catalogo.model.TipoMidia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação JDBC do {@link ItemDAO}.
 * <p>
 * Boas práticas aplicadas:
 * <ul>
 *   <li><b>try-with-resources</b>: Connection, PreparedStatement e ResultSet são sempre fechados;</li>
 *   <li><b>PreparedStatement</b>: parâmetros nunca são concatenados no SQL (evita SQL Injection);</li>
 *   <li><b>tradução de exceções</b>: toda {@link SQLException} é registrada no log com
 *       SQLState/código e convertida em {@link PersistenciaException} com mensagem amigável.</li>
 * </ul>
 */
public class ItemDAOJdbc implements ItemDAO {

    private static final Logger LOG = Logger.getLogger(ItemDAOJdbc.class.getName());

    /** Código de erro do MySQL para chave duplicada (ER_DUP_ENTRY). */
    private static final int MYSQL_CHAVE_DUPLICADA = 1062;
    /** Código de erro do MySQL para violação de CHECK (ER_CHECK_CONSTRAINT_VIOLATED). */
    private static final int MYSQL_CHECK_VIOLADO = 3819;
    /** SQLState padrão (SQL:2003) para violação de UNIQUE, usado pelo H2 e outros bancos. */
    private static final String SQLSTATE_UNIQUE = "23505";

    private static final String COLUNAS =
            "id, titulo, tipo, genero, ano_lancamento, criador, nota, sinopse, data_cadastro";

    private static final String SQL_INSERIR =
            "INSERT INTO item_catalogo (titulo, tipo, genero, ano_lancamento, criador, nota, sinopse) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_ATUALIZAR =
            "UPDATE item_catalogo SET titulo = ?, tipo = ?, genero = ?, ano_lancamento = ?, "
                    + "criador = ?, nota = ?, sinopse = ? WHERE id = ?";
    private static final String SQL_EXCLUIR = "DELETE FROM item_catalogo WHERE id = ?";
    private static final String SQL_BUSCAR_POR_ID = "SELECT " + COLUNAS + " FROM item_catalogo WHERE id = ?";
    private static final String SQL_LISTAR = "SELECT " + COLUNAS + " FROM item_catalogo WHERE 1 = 1";
    private static final String SQL_DUPLICADO =
            "SELECT COUNT(*) FROM item_catalogo "
                    + "WHERE LOWER(titulo) = LOWER(?) AND tipo = ? AND ano_lancamento = ? AND id <> ?";
    private static final String SQL_CONTAR_POR_TIPO =
            "SELECT tipo, COUNT(*) AS total FROM item_catalogo GROUP BY tipo";

    private final ConnectionProvider conexoes;

    public ItemDAOJdbc(ConnectionProvider conexoes) {
        this.conexoes = Objects.requireNonNull(conexoes, "conexoes");
    }

    @Override
    public ItemCatalogo inserir(ItemCatalogo item) {
        try (Connection con = conexoes.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            preencherParametros(ps, item);
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    item.setId(chaves.getInt(1));
                }
            }
            LOG.fine(() -> "Item inserido: " + item);
            return item;
        } catch (SQLException e) {
            throw traduzir("cadastrar o item", e);
        }
    }

    @Override
    public boolean atualizar(ItemCatalogo item) {
        Objects.requireNonNull(item.getId(), "Item sem id não pode ser atualizado");
        try (Connection con = conexoes.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ATUALIZAR)) {
            int proximo = preencherParametros(ps, item);
            ps.setInt(proximo, item.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw traduzir("atualizar o item", e);
        }
    }

    @Override
    public boolean excluir(int id) {
        try (Connection con = conexoes.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_EXCLUIR)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw traduzir("excluir o item", e);
        }
    }

    @Override
    public Optional<ItemCatalogo> buscarPorId(int id) {
        try (Connection con = conexoes.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_BUSCAR_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw traduzir("consultar o item", e);
        }
    }

    @Override
    public List<ItemCatalogo> listar(String termoBusca, TipoMidia tipo) {
        StringBuilder sql = new StringBuilder(SQL_LISTAR);
        List<Object> parametros = new ArrayList<>();

        if (termoBusca != null && !termoBusca.isBlank()) {
            String padrao = "%" + termoBusca.trim().toLowerCase(Locale.ROOT) + "%";
            sql.append(" AND (LOWER(titulo) LIKE ? OR LOWER(genero) LIKE ? OR LOWER(criador) LIKE ?)");
            parametros.add(padrao);
            parametros.add(padrao);
            parametros.add(padrao);
        }
        if (tipo != null) {
            sql.append(" AND tipo = ?");
            parametros.add(tipo.name());
        }
        sql.append(" ORDER BY titulo, ano_lancamento");

        try (Connection con = conexoes.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<ItemCatalogo> itens = new ArrayList<>();
                while (rs.next()) {
                    itens.add(mapear(rs));
                }
                return itens;
            }
        } catch (SQLException e) {
            throw traduzir("listar o catálogo", e);
        }
    }

    @Override
    public boolean existeDuplicado(String titulo, TipoMidia tipo, int anoLancamento, Integer idIgnorado) {
        try (Connection con = conexoes.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_DUPLICADO)) {
            ps.setString(1, titulo == null ? null : titulo.trim());
            ps.setString(2, tipo.name());
            ps.setInt(3, anoLancamento);
            ps.setInt(4, idIgnorado == null ? 0 : idIgnorado);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw traduzir("verificar itens duplicados", e);
        }
    }

    @Override
    public Map<TipoMidia, Integer> contarPorTipo() {
        try (Connection con = conexoes.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_CONTAR_POR_TIPO);
             ResultSet rs = ps.executeQuery()) {
            Map<TipoMidia, Integer> totais = new EnumMap<>(TipoMidia.class);
            while (rs.next()) {
                Optional<TipoMidia> tipo = TipoMidia.deTexto(rs.getString("tipo"));
                if (tipo.isPresent()) {
                    totais.put(tipo.get(), rs.getInt("total"));
                }
            }
            return totais;
        } catch (SQLException e) {
            throw traduzir("contar os itens", e);
        }
    }

    /** Preenche os 7 parâmetros comuns a INSERT e UPDATE. @return índice do próximo parâmetro */
    private int preencherParametros(PreparedStatement ps, ItemCatalogo item) throws SQLException {
        ps.setString(1, item.getTitulo());
        ps.setString(2, item.getTipo() == null ? null : item.getTipo().name());
        ps.setString(3, item.getGenero());
        if (item.getAnoLancamento() == null) {
            ps.setNull(4, Types.INTEGER);
        } else {
            ps.setInt(4, item.getAnoLancamento());
        }
        definirTexto(ps, 5, item.getCriador());
        if (item.getNota() == null) {
            ps.setNull(6, Types.DECIMAL);
        } else {
            ps.setBigDecimal(6, item.getNota());
        }
        definirTexto(ps, 7, item.getSinopse());
        return 8;
    }

    private static void definirTexto(PreparedStatement ps, int indice, String valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.VARCHAR);
        } else {
            ps.setString(indice, valor);
        }
    }

    private ItemCatalogo mapear(ResultSet rs) throws SQLException {
        ItemCatalogo item = new ItemCatalogo();
        item.setId(rs.getInt("id"));
        item.setTitulo(rs.getString("titulo"));
        String tipo = rs.getString("tipo");
        item.setTipo(TipoMidia.deTexto(tipo).orElseThrow(
                () -> new SQLException("Tipo de mídia desconhecido no banco: " + tipo)));
        item.setGenero(rs.getString("genero"));
        item.setAnoLancamento(rs.getInt("ano_lancamento"));
        item.setCriador(rs.getString("criador"));
        item.setNota(rs.getBigDecimal("nota"));
        item.setSinopse(rs.getString("sinopse"));
        Timestamp cadastro = rs.getTimestamp("data_cadastro");
        item.setDataCadastro(cadastro == null ? null : cadastro.toLocalDateTime());
        return item;
    }

    /**
     * Registra a falha técnica no log e devolve uma exceção da aplicação com
     * mensagem adequada ao usuário (sem detalhes de SQL).
     */
    private PersistenciaException traduzir(String operacao, SQLException e) {
        String detalhe = "Falha ao " + operacao + " [SQLState=" + e.getSQLState()
                + ", código=" + e.getErrorCode() + "]: " + e.getMessage();

        if (e.getErrorCode() == MYSQL_CHAVE_DUPLICADA || SQLSTATE_UNIQUE.equals(e.getSQLState())) {
            LOG.log(Level.WARNING, detalhe);
            return new RegistroDuplicadoException(
                    "Já existe um item com o mesmo título, tipo e ano de lançamento.", e);
        }
        if (e.getErrorCode() == MYSQL_CHECK_VIOLADO
                || (e.getSQLState() != null && e.getSQLState().startsWith("23"))) {
            LOG.log(Level.WARNING, detalhe, e);
            return new PersistenciaException(
                    "Os dados informados violam uma regra de integridade do banco de dados.", e);
        }
        LOG.log(Level.SEVERE, detalhe, e);
        return new PersistenciaException(
                "Não foi possível " + operacao + " no momento. Tente novamente em instantes.", e);
    }
}
