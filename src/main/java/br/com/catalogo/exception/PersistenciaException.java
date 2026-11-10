package br.com.catalogo.exception;

/**
 * Falha técnica ao acessar o banco de dados (conexão recusada, SQL inválido,
 * violação de constraint etc.). Envolve a {@link java.sql.SQLException} original
 * para que a camada web não dependa de JDBC e o usuário nunca veja um stack trace.
 */
public class PersistenciaException extends CatalogoException {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
