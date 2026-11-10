package br.com.catalogo.web;

import br.com.catalogo.model.ItemCatalogo;
import br.com.catalogo.model.TipoMidia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FormularioItem")
class FormularioItemTest {

    @Test
    @DisplayName("formulário vazio contém todos os campos em branco")
    void vazio() {
        Map<String, String> form = FormularioItem.vazio();

        assertEquals(List.of(FormularioItem.CAMPOS), List.copyOf(form.keySet()));
        assertTrue(form.values().stream().allMatch(String::isEmpty));
    }

    @Test
    @DisplayName("preenche o formulário de edição a partir do item")
    void doItem() {
        ItemCatalogo item = new ItemCatalogo("Dark", TipoMidia.SERIE, "Suspense", 2017);
        item.setId(3);
        item.setNota(new BigDecimal("9.0"));

        Map<String, String> form = FormularioItem.doItem(item);

        assertEquals("3", form.get("id"));
        assertEquals("Dark", form.get("titulo"));
        assertEquals("SERIE", form.get("tipo"));
        assertEquals("2017", form.get("anoLancamento"));
        assertEquals("9", form.get("nota"));
        assertEquals("", form.get("criador"));
        assertEquals("", form.get("sinopse"));
    }
}

