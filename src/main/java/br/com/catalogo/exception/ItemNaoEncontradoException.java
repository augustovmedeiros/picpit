package br.com.catalogo.exception;

/** O item solicitado não existe (ou foi excluído). Traduzida para HTTP 404 na camada web. */
public class ItemNaoEncontradoException extends CatalogoException {

    private static final long serialVersionUID = 1L;

    private final int id;

    public ItemNaoEncontradoException(int id) {
        super("O item #" + id + " não foi encontrado. Ele pode ter sido excluído.");
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
