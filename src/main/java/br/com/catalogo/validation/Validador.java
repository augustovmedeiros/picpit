package br.com.catalogo.validation;

import br.com.catalogo.exception.ValidacaoException;
import br.com.catalogo.model.ItemCatalogo;
import br.com.catalogo.model.TipoMidia;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Centraliza <strong>todas</strong> as regras de validação de um {@link ItemCatalogo}.
 * <p>
 * Oferece duas operações:
 * <ul>
 *   <li>{@link #converter(Map)}: transforma os textos do formulário em um objeto,
 *       acumulando erros de conversão (ex.: ano "abc") e de regra;</li>
 *   <li>{@link #validar(ItemCatalogo)}: aplica as regras a um objeto já montado
 *       (usado pela camada de serviço como "segunda barreira").</li>
 * </ul>
 * Em ambos os casos, todos os erros encontrados são reunidos em uma única
 * {@link ValidacaoException}, para que o usuário corrija tudo de uma vez.
 */
public class Validador {

    public static final int TITULO_MIN = 2;
    public static final int TITULO_MAX = 150;
    public static final int GENERO_MAX = 60;
    public static final int CRIADOR_MAX = 120;
    public static final int SINOPSE_MAX = 1000;
    public static final int ANO_MINIMO = 1800;
    public static final BigDecimal NOTA_MIN = BigDecimal.ZERO;
    public static final BigDecimal NOTA_MAX = BigDecimal.TEN;

    private final Clock relogio;

    public Validador() {
        this(Clock.systemDefaultZone());
    }

    /** Permite fixar a data nos testes (o ano máximo depende do ano atual). */
    public Validador(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /** Maior ano de lançamento aceito: o ano corrente (não se cadastram lançamentos futuros). */
    public int getAnoMaximo() {
        return Year.now(relogio).getValue();
    }

    public int getAnoMinimo() {
        return ANO_MINIMO;
    }

    /**
     * Valida um item já montado.
     *
     * @throws ValidacaoException com todos os erros encontrados
     */
    public void validar(ItemCatalogo item) throws ValidacaoException {
        if (item == null) {
            throw new ValidacaoException("item", "Nenhum item foi informado.");
        }
        Map<String, String> erros = new LinkedHashMap<>();
        aplicarRegras(item, erros);
        lancarSeHouverErros(erros);
    }

    /**
     * Converte os campos de texto do formulário em um {@link ItemCatalogo} válido.
     *
     * @param campos mapa nome-do-campo → valor digitado (pode conter nulos e espaços)
     * @return item preenchido e válido
     * @throws ValidacaoException com erros de conversão e de regra, por campo
     */
    public ItemCatalogo converter(Map<String, String> campos) throws ValidacaoException {
        Objects.requireNonNull(campos, "campos");
        Map<String, String> erros = new LinkedHashMap<>();
        ItemCatalogo item = new ItemCatalogo();

        String id = texto(campos, "id");
        if (id != null) {
            try {
                int valor = Integer.parseInt(id);
                if (valor <= 0) {
                    throw new NumberFormatException("id não positivo");
                }
                item.setId(valor);
            } catch (NumberFormatException e) {
                erros.put("id", "Identificador do item inválido.");
            }
        }

        item.setTitulo(texto(campos, "titulo"));

        String tipo = texto(campos, "tipo");
        if (tipo != null) {
            TipoMidia.deTexto(tipo).ifPresentOrElse(item::setTipo,
                    () -> erros.put("tipo", "Tipo inválido. Escolha Livro, Série ou Filme."));
        }

        item.setGenero(texto(campos, "genero"));

        String ano = texto(campos, "anoLancamento");
        if (ano != null) {
            try {
                item.setAnoLancamento(Integer.valueOf(ano));
            } catch (NumberFormatException e) {
                erros.put("anoLancamento", "O ano deve ser um número inteiro (ex.: 2010).");
            }
        }

        item.setCriador(texto(campos, "criador"));

        String nota = texto(campos, "nota");
        if (nota != null) {
            try {
                item.setNota(new BigDecimal(nota.replace(',', '.')));
            } catch (NumberFormatException e) {
                erros.put("nota", "A nota deve ser um número entre 0 e 10 (ex.: 8,5).");
            }
        }

        item.setSinopse(texto(campos, "sinopse"));

        aplicarRegras(item, erros);
        lancarSeHouverErros(erros);
        return item;
    }

    /**
     * Regras de negócio. Usa {@code putIfAbsent} para não sobrescrever um erro de
     * conversão já registrado para o mesmo campo.
     */
    private void aplicarRegras(ItemCatalogo item, Map<String, String> erros) {
        String titulo = item.getTitulo();
        if (vazio(titulo)) {
            erros.putIfAbsent("titulo", "O título é obrigatório.");
        } else if (titulo.trim().length() < TITULO_MIN) {
            erros.putIfAbsent("titulo", "O título deve ter pelo menos " + TITULO_MIN + " caracteres.");
        } else if (titulo.length() > TITULO_MAX) {
            erros.putIfAbsent("titulo", "O título deve ter no máximo " + TITULO_MAX + " caracteres.");
        }

        if (item.getTipo() == null) {
            erros.putIfAbsent("tipo", "Selecione o tipo: Livro, Série ou Filme.");
        }

        String genero = item.getGenero();
        if (vazio(genero)) {
            erros.putIfAbsent("genero", "O gênero é obrigatório.");
        } else if (genero.length() > GENERO_MAX) {
            erros.putIfAbsent("genero", "O gênero deve ter no máximo " + GENERO_MAX + " caracteres.");
        }

        Integer ano = item.getAnoLancamento();
        int anoMaximo = getAnoMaximo();
        if (ano == null) {
            erros.putIfAbsent("anoLancamento", "O ano de lançamento é obrigatório.");
        } else if (ano < ANO_MINIMO || ano > anoMaximo) {
            erros.putIfAbsent("anoLancamento",
                    "O ano deve estar entre " + ANO_MINIMO + " e " + anoMaximo + ".");
        }

        String criador = item.getCriador();
        if (criador != null && criador.length() > CRIADOR_MAX) {
            erros.putIfAbsent("criador", "Este campo deve ter no máximo " + CRIADOR_MAX + " caracteres.");
        }

        BigDecimal nota = item.getNota();
        if (nota != null) {
            if (nota.compareTo(NOTA_MIN) < 0 || nota.compareTo(NOTA_MAX) > 0) {
                erros.putIfAbsent("nota", "A nota deve estar entre 0 e 10.");
            } else if (nota.stripTrailingZeros().scale() > 1) {
                erros.putIfAbsent("nota", "A nota deve ter no máximo uma casa decimal (ex.: 7,5).");
            }
        }

        String sinopse = item.getSinopse();
        if (sinopse != null && sinopse.length() > SINOPSE_MAX) {
            erros.putIfAbsent("sinopse", "A sinopse deve ter no máximo " + SINOPSE_MAX + " caracteres.");
        }
    }

    private static void lancarSeHouverErros(Map<String, String> erros) throws ValidacaoException {
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
    }

    /** Lê um campo do formulário removendo espaços; texto vazio vira {@code null}. */
    private static String texto(Map<String, String> campos, String chave) {
        String valor = campos.get(chave);
        if (valor == null) {
            return null;
        }
        valor = valor.trim();
        return valor.isEmpty() ? null : valor;
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
