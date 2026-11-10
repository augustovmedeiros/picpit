package br.com.catalogo.web;

import br.com.catalogo.model.ItemCatalogo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Representa os valores do formulário como texto ({@code campo -> valor}).
 * <p>
 * Quando a validação falha, o formulário é reexibido com exatamente o que o
 * usuário digitou, inclusive valores inválidos, para que nada se perca.
 */
public final class FormularioItem {

    public static final String[] CAMPOS =
            {"id", "titulo", "tipo", "genero", "anoLancamento", "criador", "nota", "sinopse"};

    private FormularioItem() {
    }

    public static Map<String, String> vazio() {
        Map<String, String> form = new LinkedHashMap<>();
        for (String campo : CAMPOS) {
            form.put(campo, "");
        }
        return form;
    }

    public static Map<String, String> daRequisicao(HttpServletRequest req) {
        Map<String, String> form = new LinkedHashMap<>();
        for (String campo : CAMPOS) {
            String valor = req.getParameter(campo);
            form.put(campo, valor == null ? "" : valor);
        }
        return Collections.unmodifiableMap(form);
    }

    public static Map<String, String> doItem(ItemCatalogo item) {
        Map<String, String> form = vazio();
        form.put("id", item.getId() == null ? "" : String.valueOf(item.getId()));
        form.put("titulo", texto(item.getTitulo()));
        form.put("tipo", item.getTipo() == null ? "" : item.getTipo().name());
        form.put("genero", texto(item.getGenero()));
        form.put("anoLancamento", item.getAnoLancamento() == null ? "" : String.valueOf(item.getAnoLancamento()));
        form.put("criador", texto(item.getCriador()));
        form.put("nota", item.getNota() == null ? "" : item.getNota().stripTrailingZeros().toPlainString());
        form.put("sinopse", texto(item.getSinopse()));
        return form;
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
