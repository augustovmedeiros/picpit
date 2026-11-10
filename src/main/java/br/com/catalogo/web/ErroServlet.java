package br.com.catalogo.web;

import br.com.catalogo.exception.PersistenciaException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Página de erro global (configurada no web.xml para qualquer exceção ou código HTTP).
 * <p>
 * Gera um <b>protocolo</b> curto, registra no log o stack trace completo junto com esse
 * protocolo e mostra ao usuário apenas uma mensagem amigável e o protocolo — nunca
 * detalhes técnicos. Assim, o suporte consegue localizar o erro no log a partir do
 * que o usuário informar.
 */
@WebServlet(name = "ErroServlet", urlPatterns = "/erro")
public class ErroServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ErroServlet.class.getName());
    private static final String VIEW_ERRO = "/WEB-INF/views/error.jsp";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer statusAtributo = (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Throwable erro = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String mensagemOriginal = (String) req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String uri = (String) req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int status = statusAtributo != null ? statusAtributo
                : (erro != null ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_NOT_FOUND);
        String protocolo = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);

        if (erro != null) {
            LOG.log(Level.SEVERE, "Erro não tratado [protocolo " + protocolo + "] em " + uri, erro);
        } else if (status >= 500) {
            LOG.severe("HTTP " + status + " [protocolo " + protocolo + "] em " + uri + ": " + mensagemOriginal);
        } else {
            LOG.fine(() -> "HTTP " + status + " em " + uri + ": " + mensagemOriginal);
        }

        req.setAttribute("status", status);
        req.setAttribute("titulo", titulo(status));
        req.setAttribute("mensagem", mensagemParaUsuario(status, erro, mensagemOriginal));
        req.setAttribute("protocolo", status >= 500 ? protocolo : null);

        resp.setStatus(status);
        req.getRequestDispatcher(VIEW_ERRO).forward(req, resp);
    }

    private static String titulo(int status) {
        return switch (status) {
            case 400 -> "Requisição inválida";
            case 403 -> "Acesso negado";
            case 404 -> "Página não encontrada";
            case 405 -> "Operação não permitida";
            case 503 -> "Serviço indisponível";
            default -> status >= 500 ? "Algo deu errado" : "Não foi possível concluir a operação";
        };
    }

    /**
     * Para erros 4xx usamos a mensagem definida pela própria aplicação (via sendError).
     * Para 5xx só exibimos mensagens de exceções da aplicação, escritas para o usuário;
     * qualquer outra exceção recebe um texto genérico.
     */
    private static String mensagemParaUsuario(int status, Throwable erro, String mensagemOriginal) {
        PersistenciaException persistencia = procurarCausa(erro, PersistenciaException.class);
        if (persistencia != null) {
            return persistencia.getMessage();
        }
        if (status < 500 && mensagemOriginal != null && !mensagemOriginal.isBlank()) {
            return mensagemOriginal;
        }
        return switch (status) {
            case 404 -> "O endereço acessado não existe ou foi removido.";
            case 405 -> "Este endereço não aceita a operação solicitada.";
            default -> "Ocorreu um erro inesperado. Nossa equipe foi notificada; tente novamente em instantes.";
        };
    }

    private static <T extends Throwable> T procurarCausa(Throwable erro, Class<T> tipo) {
        Throwable atual = erro;
        int profundidade = 0;
        while (atual != null && profundidade++ < 20) {
            if (tipo.isInstance(atual)) {
                return tipo.cast(atual);
            }
            Throwable proxima = atual instanceof ServletException se && se.getRootCause() != null
                    ? se.getRootCause() : atual.getCause();
            if (proxima == atual) {
                break;
            }
            atual = proxima;
        }
        return null;
    }
}
