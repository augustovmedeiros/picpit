<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="tituloPagina" value="Catálogo"/>
<%@ include file="fragmentos/cabecalho.jspf" %>

<div class="cabecalho-pagina">
    <div>
        <h1>Meu catálogo</h1>
        <p class="subtitulo">Livros, séries e filmes cadastrados, com nota de 0 a 10.</p>
    </div>
    <a class="botao botao--primario" href="<c:url value='/itens/novo'/>">+ Cadastrar item</a>
</div>

<c:if test="${not empty mensagemFlash}">
    <div class="alerta alerta--sucesso" role="status" data-fechavel>
        <span class="alerta__icone" aria-hidden="true">✓</span>
        <p><c:out value="${mensagemFlash}"/></p>
        <button type="button" class="alerta__fechar" aria-label="Fechar mensagem" hidden>×</button>
    </div>
</c:if>

<%-- Resumo por tipo: cada cartão também funciona como filtro --%>
<section class="resumo" aria-label="Resumo do catálogo">
    <a class="resumo__cartao ${empty tipoSelecionado ? 'resumo__cartao--ativo' : ''}"
       href="<c:url value='/itens'><c:param name='busca' value='${busca}'/></c:url>">
        <span class="resumo__numero">${totalGeral}</span>
        <span class="resumo__rotulo">Todos</span>
    </a>
    <c:forEach var="t" items="${tipos}">
        <a class="resumo__cartao resumo__cartao--${fn:toLowerCase(t.codigo)} ${t eq tipoSelecionado ? 'resumo__cartao--ativo' : ''}"
           href="<c:url value='/itens'><c:param name='tipo' value='${t.codigo}'/><c:param name='busca' value='${busca}'/></c:url>">
            <span class="resumo__numero">${resumo[t]}</span>
            <span class="resumo__rotulo"><c:out value="${t.descricao}"/>${resumo[t] eq 1 ? '' : 's'}</span>
        </a>
    </c:forEach>
</section>

<form class="filtros" method="get" action="<c:url value='/itens'/>" role="search">
    <div class="filtros__campo filtros__campo--busca">
        <label for="busca">Buscar</label>
        <input type="search" id="busca" name="busca" maxlength="100"
               placeholder="Título, gênero, autor, criador ou diretor"
               value="<c:out value='${busca}'/>">
    </div>
    <div class="filtros__campo">
        <label for="filtro-tipo">Tipo</label>
        <select id="filtro-tipo" name="tipo">
            <option value="">Todos</option>
            <c:forEach var="t" items="${tipos}">
                <option value="${t.codigo}" ${t eq tipoSelecionado ? 'selected' : ''}><c:out value="${t.descricao}"/></option>
            </c:forEach>
        </select>
    </div>
    <div class="filtros__acoes">
        <button type="submit" class="botao botao--secundario">Filtrar</button>
        <c:if test="${not empty busca or not empty tipoSelecionado}">
            <a class="botao botao--texto" href="<c:url value='/itens'/>">Limpar</a>
        </c:if>
    </div>
</form>

<c:choose>
    <c:when test="${empty itens}">
        <div class="vazio">
            <p class="vazio__icone" aria-hidden="true">∅</p>
            <c:choose>
                <c:when test="${not empty busca or not empty tipoSelecionado}">
                    <h2>Nenhum item encontrado</h2>
                    <p>Nenhum item corresponde aos filtros informados.</p>
                    <a class="botao botao--secundario" href="<c:url value='/itens'/>">Ver todo o catálogo</a>
                </c:when>
                <c:otherwise>
                    <h2>O catálogo está vazio</h2>
                    <p>Cadastre o primeiro livro, série ou filme.</p>
                    <a class="botao botao--primario" href="<c:url value='/itens/novo'/>">+ Cadastrar item</a>
                </c:otherwise>
            </c:choose>
        </div>
    </c:when>
    <c:otherwise>
        <p class="contagem">
            ${fn:length(itens)} ${fn:length(itens) eq 1 ? 'item encontrado' : 'itens encontrados'}
        </p>
        <div class="tabela-container">
            <table class="tabela">
                <thead>
                <tr>
                    <th scope="col">Título</th>
                    <th scope="col">Tipo</th>
                    <th scope="col">Gênero</th>
                    <th scope="col" class="num">Ano</th>
                    <th scope="col">Autor / Criador / Diretor</th>
                    <th scope="col" class="num">Nota</th>
                    <th scope="col" class="acoes"><span class="sr-only">Ações</span></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="item" items="${itens}">
                    <tr>
                        <td data-rotulo="Título">
                            <span class="tabela__titulo"><c:out value="${item.titulo}"/></span>
                            <c:if test="${not empty item.sinopse}">
                                <span class="tabela__sinopse"><c:out value="${item.sinopse}"/></span>
                            </c:if>
                        </td>
                        <td data-rotulo="Tipo">
                            <span class="selo selo--${fn:toLowerCase(item.tipo.codigo)}"><c:out value="${item.tipo.descricao}"/></span>
                        </td>
                        <td data-rotulo="Gênero"><c:out value="${item.genero}"/></td>
                        <td data-rotulo="Ano" class="num">${item.anoLancamento}</td>
                        <td data-rotulo="${item.tipo.rotuloCriador}">
                            <c:out value="${item.criador}" default="—"/>
                        </td>
                        <td data-rotulo="Nota" class="num">
                            <c:choose>
                                <c:when test="${item.nota ne null}">
                                    <span class="nota ${item.nota ge 8 ? 'nota--alta' : (item.nota ge 5 ? 'nota--media' : 'nota--baixa')}">
                                        <fmt:formatNumber value="${item.nota}" minFractionDigits="1" maxFractionDigits="1"/>
                                    </span>
                                </c:when>
                                <c:otherwise><span class="sem-nota">—</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="acoes">
                            <a class="botao botao--texto botao--pequeno"
                               href="<c:url value='/itens/editar'><c:param name='id' value='${item.id}'/></c:url>">Editar</a>
                            <form method="post" action="<c:url value='/itens/excluir'/>" class="form-inline"
                                  data-confirmar="Excluir &quot;${fn:escapeXml(item.titulo)}&quot; do catálogo? Esta ação não pode ser desfeita.">
                                <input type="hidden" name="id" value="${item.id}">
                                <button type="submit" class="botao botao--perigo botao--pequeno">Excluir</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="fragmentos/rodape.jspf" %>

