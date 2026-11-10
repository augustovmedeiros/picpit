package br.com.catalogo.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Fábrica de conexões baseada no {@link DriverManager}.
 * <p>
 * Cada chamada abre uma conexão nova, que deve ser fechada por quem a usa
 * (os DAOs fazem isso com <em>try-with-resources</em>, garantindo que nenhuma
 * conexão fique aberta mesmo quando ocorre uma exceção).
 */
public class ConnectionFactory implements ConnectionProvider {

    private final String url;
    private final String usuario;
    private final String senha;

    public ConnectionFactory(String url, String usuario, String senha) {
        this.url = Objects.requireNonNull(url, "url");
        this.usuario = usuario;
        this.senha = senha;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    /** URL sem credenciais, segura para aparecer em logs. */
    public String getUrl() {
        return url;
    }
}
