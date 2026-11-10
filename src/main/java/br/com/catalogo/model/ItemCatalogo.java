package br.com.catalogo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Entidade que representa um item do catálogo (livro, série ou filme).
 * <p>
 * Mapeada para a tabela {@code item_catalogo}. Os atributos são encapsulados e o
 * objeto é validado pela classe {@link br.com.catalogo.validation.Validador}
 * antes de chegar à camada de persistência.
 */
public class ItemCatalogo {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Integer id;
    private String titulo;
    private TipoMidia tipo;
    private String genero;
    private Integer anoLancamento;
    private String criador;
    private BigDecimal nota;
    private String sinopse;
    private LocalDateTime dataCadastro;

    public ItemCatalogo() {
    }

    public ItemCatalogo(String titulo, TipoMidia tipo, String genero, Integer anoLancamento) {
        this.titulo = titulo;
        this.tipo = tipo;
        this.genero = genero;
        this.anoLancamento = anoLancamento;
    }

    public boolean isNovo() {
        return id == null;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public TipoMidia getTipo() {
        return tipo;
    }

    public void setTipo(TipoMidia tipo) {
        this.tipo = tipo;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public Integer getAnoLancamento() {
        return anoLancamento;
    }

    public void setAnoLancamento(Integer anoLancamento) {
        this.anoLancamento = anoLancamento;
    }

    public String getCriador() {
        return criador;
    }

    public void setCriador(String criador) {
        this.criador = criador;
    }

    public BigDecimal getNota() {
        return nota;
    }

    public void setNota(BigDecimal nota) {
        this.nota = nota;
    }

    public String getSinopse() {
        return sinopse;
    }

    public void setSinopse(String sinopse) {
        this.sinopse = sinopse;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    /** Data de cadastro formatada para exibição (JSTL não formata {@link LocalDateTime}). */
    public String getDataCadastroFormatada() {
        return dataCadastro == null ? "" : dataCadastro.format(FORMATO_DATA);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemCatalogo outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ItemCatalogo{id=" + id + ", titulo='" + titulo + "', tipo=" + tipo
                + ", ano=" + anoLancamento + '}';
    }
}
