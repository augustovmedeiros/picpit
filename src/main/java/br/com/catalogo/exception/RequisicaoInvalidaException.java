package br.com.catalogo.exception;

/** Parâmetro de requisição ausente ou malformado (ex.: {@code ?id=abc}). Traduzida para HTTP 400. */
public class RequisicaoInvalidaException extends CatalogoException {

    private static final long serialVersionUID = 1L;

    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
