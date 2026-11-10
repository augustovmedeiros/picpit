package br.com.catalogo.exception;

/**
 * Lançada quando o banco rejeita uma gravação por violar a chave única
 * (título + tipo + ano). A camada de serviço converte esta exceção em uma
 * {@link ValidacaoException} associada ao campo "titulo".
 */
public class RegistroDuplicadoException extends PersistenciaException {

    private static final long serialVersionUID = 1L;

    public RegistroDuplicadoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
