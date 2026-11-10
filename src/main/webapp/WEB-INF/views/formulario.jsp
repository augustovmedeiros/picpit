<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="tituloPagina" value="${edicao ? 'Editar item' : 'Novo item'}"/>
<%@ include file="fragmentos/cabecalho.jspf" %>

<%-- Rótulo do campo "criador" conforme o tipo selecionado (Autor, Criador ou Diretor) --%>
<c:set var="rotuloCriador" value="Autor(a), criador(a) ou diretor(a)"/>
<c:forEach var="t" items="${tipos}">
    <c:if test="${t.codigo eq form.tipo}"><c:set var="rotuloCriador" value="${t.rotuloCriador}"/></c:if>
</c:forEach>

<nav class="trilha" aria-label="Trilha de navegação">
    <a href="<c:url value='/itens'/>">Catálogo</a> <span aria-hidden="true">›</span> <span>${tituloPagina}</span>
</nav>

<div class="cabecalho-pagina">
    <div>
        <h1>${tituloPagina}</h1>
        <p class="subtitulo">Campos marcados com <span class="obrigatorio" aria-hidden="true">*</span> são obrigatórios.</p>
    </div>
</div>

<%-- Resumo dos erros devolvidos pelo servidor (ValidacaoException) --%>
<c:if test="${not empty erros}">
    <div class="alerta alerta--erro" role="alert" tabindex="-1" id="resumo-erros">
        <span class="alerta__icone" aria-hidden="true">!</span>
        <div>
            <p><strong>Não foi possível salvar.</strong>
                ${fn:length(erros) eq 1 ? 'Corrija o campo indicado' : 'Corrija os campos indicados'} e tente novamente.</p>
            <ul class="alerta__lista">
                <c:forEach var="erro" items="${erros}">
                    <li><a href="#${erro.key}"><c:out value="${erro.value}"/></a></li>
                </c:forEach>
            </ul>
        </div>
    </div>
</c:if>

<form class="formulario cartao" method="post" action="<c:url value='/itens/salvar'/>" data-validar>
    <input type="hidden" name="id" value="<c:out value='${form.id}'/>">

    <div class="campo ${not empty erros.titulo ? 'campo--invalido' : ''}">
        <label for="titulo">Título <span class="obrigatorio" aria-hidden="true">*</span></label>
        <input type="text" id="titulo" name="titulo" required minlength="2" maxlength="150"
               autocomplete="off" value="<c:out value='${form.titulo}'/>"
               aria-describedby="erro-titulo" ${not empty erros.titulo ? 'aria-invalid="true"' : ''}>
        <p class="campo__erro" id="erro-titulo" aria-live="polite"><c:out value="${erros.titulo}"/></p>
    </div>

    <div class="grade grade--2">
        <div class="campo ${not empty erros.tipo ? 'campo--invalido' : ''}">
            <label for="tipo">Tipo <span class="obrigatorio" aria-hidden="true">*</span></label>
            <select id="tipo" name="tipo" required aria-describedby="erro-tipo"
                    ${not empty erros.tipo ? 'aria-invalid="true"' : ''}>
                <option value="">Selecione…</option>
                <c:forEach var="t" items="${tipos}">
                    <option value="${t.codigo}" data-rotulo-criador="${t.rotuloCriador}"
                        ${t.codigo eq form.tipo ? 'selected' : ''}><c:out value="${t.descricao}"/></option>
                </c:forEach>
            </select>
            <p class="campo__erro" id="erro-tipo" aria-live="polite"><c:out value="${erros.tipo}"/></p>
        </div>

        <div class="campo ${not empty erros.genero ? 'campo--invalido' : ''}">
            <label for="genero">Gênero <span class="obrigatorio" aria-hidden="true">*</span></label>
            <input type="text" id="genero" name="genero" required maxlength="60" list="generos"
                   autocomplete="off" value="<c:out value='${form.genero}'/>"
                   aria-describedby="erro-genero" ${not empty erros.genero ? 'aria-invalid="true"' : ''}>
            <datalist id="generos">
                <option value="Aventura"></option>
                <option value="Ação"></option>
                <option value="Animação"></option>
                <option value="Biografia"></option>
                <option value="Comédia"></option>
                <option value="Documentário"></option>
                <option value="Drama"></option>
                <option value="Fantasia"></option>
                <option value="Ficção científica"></option>
                <option value="Mistério"></option>
                <option value="Romance"></option>
                <option value="Suspense"></option>
                <option value="Terror"></option>
            </datalist>
            <p class="campo__erro" id="erro-genero" aria-live="polite"><c:out value="${erros.genero}"/></p>
        </div>
    </div>

    <div class="grade grade--2">
        <div class="campo ${not empty erros.anoLancamento ? 'campo--invalido' : ''}">
            <label for="anoLancamento">Ano de lançamento <span class="obrigatorio" aria-hidden="true">*</span></label>
            <input type="number" id="anoLancamento" name="anoLancamento" required
                   min="${anoMinimo}" max="${anoMaximo}" step="1" inputmode="numeric"
                   placeholder="Ex.: ${anoMaximo - 10}" value="<c:out value='${form.anoLancamento}'/>"
                   aria-describedby="dica-ano erro-anoLancamento"
                   ${not empty erros.anoLancamento ? 'aria-invalid="true"' : ''}>
            <p class="campo__dica" id="dica-ano">Entre ${anoMinimo} e ${anoMaximo}.</p>
            <p class="campo__erro" id="erro-anoLancamento" aria-live="polite"><c:out value="${erros.anoLancamento}"/></p>
        </div>

        <div class="campo ${not empty erros.nota ? 'campo--invalido' : ''}">
            <label for="nota">Nota</label>
            <input type="number" id="nota" name="nota" min="0" max="10" step="0.1" inputmode="decimal"
                   placeholder="Ex.: 8.5" value="<c:out value='${form.nota}'/>"
                   aria-describedby="dica-nota erro-nota" ${not empty erros.nota ? 'aria-invalid="true"' : ''}>
            <p class="campo__dica" id="dica-nota">Opcional. De 0 a 10, com até uma casa decimal.</p>
            <p class="campo__erro" id="erro-nota" aria-live="polite"><c:out value="${erros.nota}"/></p>
        </div>
    </div>

    <div class="campo ${not empty erros.criador ? 'campo--invalido' : ''}">
        <label for="criador"><span id="rotulo-criador"><c:out value="${rotuloCriador}"/></span></label>
        <input type="text" id="criador" name="criador" maxlength="120" autocomplete="off"
               value="<c:out value='${form.criador}'/>"
               aria-describedby="erro-criador" ${not empty erros.criador ? 'aria-invalid="true"' : ''}>
        <p class="campo__erro" id="erro-criador" aria-live="polite"><c:out value="${erros.criador}"/></p>
    </div>

    <div class="campo ${not empty erros.sinopse ? 'campo--invalido' : ''}">
        <label for="sinopse">Sinopse</label>
        <textarea id="sinopse" name="sinopse" rows="4" maxlength="1000" data-contador="contador-sinopse"
                  aria-describedby="contador-sinopse erro-sinopse"
                  ${not empty erros.sinopse ? 'aria-invalid="true"' : ''}><c:out value="${form.sinopse}"/></textarea>
        <p class="campo__dica campo__dica--direita" id="contador-sinopse">${fn:length(form.sinopse)}/1000 caracteres</p>
        <p class="campo__erro" id="erro-sinopse" aria-live="polite"><c:out value="${erros.sinopse}"/></p>
    </div>

    <c:if test="${not empty erros.id}">
        <p class="campo__erro"><c:out value="${erros.id}"/></p>
    </c:if>

    <div class="formulario__acoes">
        <a class="botao botao--texto" href="<c:url value='/itens'/>">Cancelar</a>
        <button type="submit" class="botao botao--primario">${edicao ? 'Salvar alterações' : 'Cadastrar'}</button>
    </div>
</form>

<%@ include file="fragmentos/rodape.jspf" %>

