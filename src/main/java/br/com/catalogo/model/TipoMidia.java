package br.com.catalogo.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Tipos de mídia aceitos pelo catálogo.
 * <p>
 * O valor gravado no banco é o {@link #name()} da constante (LIVRO, SERIE, FILME),
 * protegido também por uma constraint CHECK na tabela.
 */
public enum TipoMidia {

    LIVRO("Livro", "Autor(a)"),
    SERIE("Série", "Criador(a)"),
    FILME("Filme", "Diretor(a)");

    private final String descricao;
    private final String rotuloCriador;

    TipoMidia(String descricao, String rotuloCriador) {
        this.descricao = descricao;
        this.rotuloCriador = rotuloCriador;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Rótulo usado para o campo "criador" de acordo com o tipo (autor, criador ou diretor). */
    public String getRotuloCriador() {
        return rotuloCriador;
    }

    /** Necessário para acessar o nome da constante via EL nas JSPs. */
    public String getCodigo() {
        return name();
    }

    /**
     * Converte um texto vindo do formulário em {@code TipoMidia} sem lançar exceção.
     *
     * @return o tipo correspondente ou {@link Optional#empty()} se o valor for vazio ou inválido
     */
    public static Optional<TipoMidia> deTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(valor.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
