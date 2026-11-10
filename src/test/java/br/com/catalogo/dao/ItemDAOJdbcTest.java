package br.com.catalogo.dao;

import br.com.catalogo.exception.PersistenciaException;
import br.com.catalogo.exception.RegistroDuplicadoException;
import br.com.catalogo.model.ItemCatalogo;
import br.com.catalogo.model.TipoMidia;
import br.com.catalogo.util.ScriptSqlRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de integração do DAO JDBC com H2 em memória (modo MySQL), usando o
 * mesmo {@code db/schema.sql} da aplicação. Verificam também que as constraints
 * do banco funcionam como última barreira e que nenhuma conexão fica aberta.
 */
@DisplayName("ItemDAOJdbc (integração com H2)")
class ItemDAOJdbcTest {

    private ConexoesMonitoradas conexoes;
    private ItemDAOJdbc dao;
    private Connection manterBancoVivo;

    @BeforeEach
    void preparar() throws Exception {
        String url = "jdbc:h2:mem:teste_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
        ConnectionFactory fabrica = new ConnectionFactory(url, "sa", "");
        // Mantém o banco em memória vivo durante o teste (H2 o descarta ao fechar a última conexão).
        manterBancoVivo = fabrica.getConnection();
        ScriptSqlRunner.executar(fabrica, "db/schema.sql");

        conexoes = new ConexoesMonitoradas(fabrica);
        dao = new ItemDAOJdbc(conexoes);
    }

    @AfterEach
    void encerrar() throws SQLException {
        assertEquals(0, conexoes.abertas(), "Todas as conexões devem ser fechadas pelo DAO");
        manterBancoVivo.close();
    }

    private static ItemCatalogo novo(String titulo, TipoMidia tipo, String genero, int ano) {
        return new ItemCatalogo(titulo, tipo, genero, ano);
    }

    @Test
    @DisplayName("insere, busca, atualiza e exclui (CRUD completo)")
    void crud() {
        ItemCatalogo item = novo("Dom Casmurro", TipoMidia.LIVRO, "Romance", 1899);
        item.setCriador("Machado de Assis");
        item.setNota(new BigDecimal("9.5"));

        dao.inserir(item);
        assertNotNull(item.getId());

        ItemCatalogo lido = dao.buscarPorId(item.getId()).orElseThrow();
        assertEquals("Dom Casmurro", lido.getTitulo());
        assertEquals(TipoMidia.LIVRO, lido.getTipo());
        assertEquals(0, new BigDecimal("9.5").compareTo(lido.getNota()));
        assertNotNull(lido.getDataCadastro());

        lido.setNota(new BigDecimal("10"));
        lido.setSinopse("Clássico da literatura brasileira.");
        assertTrue(dao.atualizar(lido));
        assertEquals("Clássico da literatura brasileira.", dao.buscarPorId(item.getId()).orElseThrow().getSinopse());

        assertTrue(dao.excluir(item.getId()));
        assertEquals(Optional.empty(), dao.buscarPorId(item.getId()));
        assertTrue(conexoes.totalAbertas() >= 6);
    }

    @Test
    @DisplayName("atualizar/excluir id inexistente devolve false")
    void inexistente() {
        ItemCatalogo fantasma = novo("Fantasma", TipoMidia.FILME, "Terror", 2000);
        fantasma.setId(999);

        assertFalse(dao.atualizar(fantasma));
        assertFalse(dao.excluir(999));
    }

    @Test
    @DisplayName("lista ordenado por título com filtros por termo e por tipo")
    void listarComFiltros() {
        dao.inserir(novo("Interestelar", TipoMidia.FILME, "Ficção científica", 2014));
        dao.inserir(novo("Dark", TipoMidia.SERIE, "Ficção científica", 2017));
        dao.inserir(novo("Cidade de Deus", TipoMidia.FILME, "Drama", 2002));

        List<ItemCatalogo> todos = dao.listar(null, null);
        assertEquals(List.of("Cidade de Deus", "Dark", "Interestelar"),
                todos.stream().map(ItemCatalogo::getTitulo).toList());

        assertEquals(2, dao.listar("FICÇÃO", null).size(), "busca ignora maiúsculas e procura no gênero");
        assertEquals(1, dao.listar("ficção", TipoMidia.SERIE).size());
        assertEquals(2, dao.listar(null, TipoMidia.FILME).size());
        assertTrue(dao.listar("inexistente", null).isEmpty());
    }

    @Test
    @DisplayName("termo de busca com caracteres especiais não quebra o SQL (PreparedStatement)")
    void buscaSegura() {
        dao.inserir(novo("O'Brien", TipoMidia.LIVRO, "Biografia", 1990));

        assertEquals(1, dao.listar("o'brien", null).size());
        assertTrue(dao.listar("' OR '1'='1", null).isEmpty());
        assertEquals(1, dao.listar(null, null).size(), "a tabela continua intacta");
    }

    @Test
    @DisplayName("detecta duplicados ignorando maiúsculas e o próprio item")
    void existeDuplicado() {
        ItemCatalogo dark = dao.inserir(novo("Dark", TipoMidia.SERIE, "Suspense", 2017));

        assertTrue(dao.existeDuplicado("DARK", TipoMidia.SERIE, 2017, null));
        assertFalse(dao.existeDuplicado("Dark", TipoMidia.SERIE, 2017, dark.getId()));
        assertFalse(dao.existeDuplicado("Dark", TipoMidia.FILME, 2017, null));
        assertFalse(dao.existeDuplicado("Dark", TipoMidia.SERIE, 2018, null));
    }

    @Test
    @DisplayName("conta itens por tipo")
    void contarPorTipo() {
        dao.inserir(novo("Livro A", TipoMidia.LIVRO, "Romance", 2000));
        dao.inserir(novo("Livro B", TipoMidia.LIVRO, "Romance", 2001));
        dao.inserir(novo("Filme A", TipoMidia.FILME, "Drama", 2002));

        Map<TipoMidia, Integer> totais = dao.contarPorTipo();

        assertEquals(2, totais.get(TipoMidia.LIVRO));
        assertEquals(1, totais.get(TipoMidia.FILME));
        assertFalse(totais.containsKey(TipoMidia.SERIE));
    }

    @Test
    @DisplayName("UNIQUE do banco vira RegistroDuplicadoException")
    void constraintUnique() {
        dao.inserir(novo("Duna", TipoMidia.LIVRO, "Ficção", 1965));

        PersistenciaException e = assertThrows(PersistenciaException.class,
                () -> dao.inserir(novo("Duna", TipoMidia.LIVRO, "Ficção", 1965)));

        assertInstanceOf(RegistroDuplicadoException.class, e);
        assertInstanceOf(SQLException.class, e.getCause(), "a causa técnica é preservada para o log");
        assertFalse(e.getMessage().contains("SQL"), "a mensagem ao usuário não expõe detalhes técnicos");
    }

    @Test
    @DisplayName("CHECK da nota (0 a 10) barra dados que escaparam da validação")
    void constraintCheckNota() {
        ItemCatalogo invalido = novo("Nota 11", TipoMidia.FILME, "Drama", 2000);
        invalido.setNota(new BigDecimal("11"));

        PersistenciaException e = assertThrows(PersistenciaException.class, () -> dao.inserir(invalido));

        assertTrue(e.getMessage().contains("regra de integridade"));
    }

    @Test
    @DisplayName("CHECK do título e NOT NULL barram dados inválidos")
    void constraintTituloENotNull() {
        assertThrows(PersistenciaException.class,
                () -> dao.inserir(novo("   ", TipoMidia.LIVRO, "Romance", 2000)));
        assertThrows(PersistenciaException.class,
                () -> dao.inserir(novo(null, TipoMidia.LIVRO, "Romance", 2000)));
        assertThrows(PersistenciaException.class,
                () -> dao.inserir(novo("Sem ano", TipoMidia.LIVRO, "Romance", 1500)));
    }

    @Test
    @DisplayName("CHECK do tipo barra valores fora da lista")
    void constraintTipo() throws SQLException {
        try (Connection con = conexoes.getConnection(); Statement st = con.createStatement()) {
            SQLException e = assertThrows(SQLException.class, () -> st.executeUpdate(
                    "INSERT INTO item_catalogo (titulo, tipo, genero, ano_lancamento) "
                            + "VALUES ('Revista X', 'REVISTA', 'Variedades', 2000)"));
            assertTrue(e.getSQLState().startsWith("23"));
        }
    }

    @Test
    @DisplayName("falha de conexão vira PersistenciaException com mensagem amigável")
    void falhaDeConexao() {
        ItemDAOJdbc daoSemBanco = new ItemDAOJdbc(() -> {
            throw new SQLException("Communications link failure", "08S01");
        });

        PersistenciaException e = assertThrows(PersistenciaException.class, () -> daoSemBanco.listar(null, null));

        assertEquals("Não foi possível listar o catálogo no momento. Tente novamente em instantes.", e.getMessage());
        assertEquals("08S01", ((SQLException) e.getCause()).getSQLState());
    }

    @Test
    @DisplayName("conexão é fechada mesmo quando a operação falha (try-with-resources)")
    void fechaConexaoNaFalha() {
        dao.inserir(novo("Duna", TipoMidia.LIVRO, "Ficção", 1965));
        int antes = conexoes.totalAbertas();

        assertThrows(PersistenciaException.class, () -> dao.inserir(novo("Duna", TipoMidia.LIVRO, "Ficção", 1965)));

        assertEquals(antes + 1, conexoes.totalAbertas());
        assertEquals(0, conexoes.abertas());
    }

    /**
     * Envolve as conexões em um proxy que conta quantas foram abertas e quantas
     * ainda não foram fechadas. Usado para provar que o DAO não vaza conexões.
     */
    private static final class ConexoesMonitoradas implements ConnectionProvider {

        private final ConnectionProvider real;
        private final AtomicInteger abertas = new AtomicInteger();
        private final AtomicInteger total = new AtomicInteger();

        ConexoesMonitoradas(ConnectionProvider real) {
            this.real = real;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection conexao = real.getConnection();
            abertas.incrementAndGet();
            total.incrementAndGet();
            boolean[] fechada = {false};
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, metodo, args) -> {
                        if ("close".equals(metodo.getName()) && !fechada[0]) {
                            fechada[0] = true;
                            abertas.decrementAndGet();
                        }
                        try {
                            return metodo.invoke(conexao, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
        }

        int abertas() {
            return abertas.get();
        }

        int totalAbertas() {
            return total.get();
        }
    }
}

