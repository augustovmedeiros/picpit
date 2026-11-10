package br.com.catalogo.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstração para obter conexões JDBC. Permite que os DAOs usem o MySQL em produção
 * e um banco H2 em memória nos testes, sem alterar o código.
 */
@FunctionalInterface
public interface ConnectionProvider {

    Connection getConnection() throws SQLException;
}
