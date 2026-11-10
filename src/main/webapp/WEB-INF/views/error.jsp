<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%--
    Página de erro global, preenchida pelo ErroServlet.
    Exibe apenas mensagens amigáveis: o stack trace e os detalhes técnicos
    ficam somente no log do servidor, identificados pelo protocolo.
--%>
<c:set var="tituloPagina" value="${empty titulo ? 'Erro' : titulo}"/>
<%@ include file="fragmentos/cabecalho.jspf" %>

<section class="erro cartao" aria-labelledby="erro-titulo">
    <p class="erro__codigo" aria-hidden="true">${empty status ? 'Erro' : status}</p>
    <h1 id="erro-titulo"><c:out value="${tituloPagina}"/></h1>
    <p class="erro__mensagem">
        <c:out value="${mensagem}" default="Ocorreu um erro inesperado. Tente novamente em instantes."/>
    </p>

    <c:if test="${not empty protocolo}">
        <p class="erro__protocolo">
            Se o problema persistir, informe ao suporte o protocolo
            <code><c:out value="${protocolo}"/></code>.
        </p>
    </c:if>

    <div class="erro__acoes">
        <a class="botao botao--primario" href="<c:url value='/itens'/>">Voltar ao catálogo</a>
        <a class="botao botao--texto" href="<c:url value='/itens'/>" data-voltar hidden>Página anterior</a>
    </div>
</section>

<%@ include file="fragmentos/rodape.jspf" %>
