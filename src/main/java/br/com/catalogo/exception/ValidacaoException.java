package br.com.catalogo.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exceção <strong>checada</strong> que indica dados inválidos informados pelo usuário.
 * <p>
 * Por ser checada, obriga quem chama a tratar o caso (normalmente reexibindo o
 * formulário). Carrega um mapa {@code campo -> mensagem} para que todos os erros
 * sejam mostrados de uma só vez, cada um ao lado do seu campo.
 */
public class ValidacaoException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> erros;

    public ValidacaoException(Map<String, String> erros) {
        super(montarMensagem(erros));
        this.erros = Collections.unmodifiableMap(new LinkedHashMap<>(erros));
    }

    public ValidacaoException(String campo, String mensagem) {
        this(Map.of(campo, mensagem));
    }

    /** Mapa (somente leitura e na ordem dos campos) com as mensagens de erro. */
    public Map<String, String> getErros() {
        return erros;
    }

    public String getErro(String campo) {
        return erros.get(campo);
    }

    public boolean temErro(String campo) {
        return erros.containsKey(campo);
    }

    private static String montarMensagem(Map<String, String> erros) {
        if (erros == null || erros.isEmpty()) {
            throw new IllegalArgumentException("ValidacaoException exige ao menos um erro.");
        }
        return erros.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; ", "Dados inválidos - ", ""));
    }
}
