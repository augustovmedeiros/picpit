package br.com.catalogo.exception;

/**
 * Raiz das exceções <strong>não checadas</strong> da aplicação.
 * <p>
 * Hierarquia:
 * <pre>
 * RuntimeException
 *  └── CatalogoException
 *       ├── PersistenciaException        (falhas de banco / JDBC)
 *       │    └── RegistroDuplicadoException (violação de UNIQUE)
 *       ├── ItemNaoEncontradoException   (id inexistente → HTTP 404)
 *       └── RequisicaoInvalidaException  (parâmetro malformado → HTTP 400)
 *
 * Exception
 *  └── ValidacaoException (checada: regras de negócio / dados inválidos)
 * </pre>
 * As mensagens dessas exceções são escritas para o usuário final; os detalhes
 * técnicos ficam na causa ({@link #getCause()}) e são enviados apenas ao log.
 */
public class CatalogoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CatalogoException(String mensagem) {
        super(mensagem);
    }

    public CatalogoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
