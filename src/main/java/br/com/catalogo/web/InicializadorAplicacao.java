package br.com.catalogo.web;

import br.com.catalogo.config.Configuracao;
import br.com.catalogo.dao.ConnectionFactory;
import br.com.catalogo.dao.ItemDAOJdbc;
import br.com.catalogo.service.ItemService;
import br.com.catalogo.util.ScriptSqlRunner;
import br.com.catalogo.validation.Validador;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Monta as dependências da aplicação na inicialização (injeção manual):
 * Configuração → ConnectionFactory → ItemDAOJdbc → ItemService.
 * <p>
 * O serviço fica disponível como atributo do {@code ServletContext} para os Servlets.
 */
@WebListener
public class InicializadorAplicacao implements ServletContextListener {

    public static final String ATRIBUTO_SERVICO = ItemService.class.getName();

    private static final Logger LOG = Logger.getLogger(InicializadorAplicacao.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent evento) {
        Configuracao cfg = Configuracao.carregar();

        carregarDriver(cfg.get("db.driver"));
        ConnectionFactory conexoes = new ConnectionFactory(
                cfg.obrigatorio("db.url"), cfg.get("db.usuario"), cfg.get("db.senha"));
        executarScripts(conexoes, cfg.get("db.scripts"));

        ItemService servico = new ItemService(new ItemDAOJdbc(conexoes), new Validador());
        evento.getServletContext().setAttribute(ATRIBUTO_SERVICO, servico);

        // Apenas a URL vai para o log: usuário e senha nunca são registrados.
        LOG.info(() -> "Catálogo iniciado. Banco de dados: " + conexoes.getUrl());
    }

    /**
     * Em servidores como o Tomcat, o driver dentro de WEB-INF/lib nem sempre é
     * registrado automaticamente. Uma falha aqui é registrada, mas não derruba a
     * aplicação: o usuário verá a página de erro amigável ao tentar usar o banco.
     */
    private void carregarDriver(String driver) {
        if (driver == null) {
            return;
        }
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "Driver JDBC não encontrado no classpath: " + driver, e);
        }
    }

    /** Executa scripts opcionais (ex.: criação de tabelas no modo demonstração com H2). */
    private void executarScripts(ConnectionFactory conexoes, String scripts) {
        if (scripts == null) {
            return;
        }
        for (String script : scripts.split(",")) {
            String recurso = script.trim();
            if (recurso.isEmpty()) {
                continue;
            }
            try {
                ScriptSqlRunner.executar(conexoes, recurso);
                LOG.info(() -> "Script executado: " + recurso);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Script ignorado (" + recurso + "): " + e.getMessage(), e);
            }
        }
    }
}
