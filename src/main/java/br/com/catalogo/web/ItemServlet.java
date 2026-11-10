package br.com.catalogo.web;

import br.com.catalogo.exception.ItemNaoEncontradoException;
import br.com.catalogo.exception.RequisicaoInvalidaException;
import br.com.catalogo.exception.ValidacaoException;
import br.com.catalogo.model.ItemCatalogo;
import br.com.catalogo.model.TipoMidia;
import br.com.catalogo.service.ItemService;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Controlador (MVC) do CRUD de itens.
 *
 * <pre>
 * GET  /itens                 lista (filtros: ?busca=...&amp;tipo=LIVRO|SERIE|FILME)
 * GET  /itens/novo            formulário de cadastro
 * GET  /itens/editar?id=N     formulário de edição
 * POST /itens/salvar          inclui ou altera
 * POST /itens/excluir         exclui (id no corpo)
 * </pre>
 *
 * Tratamento de exceções:
 * <ul>
 *   <li>{@link ValidacaoException} → formulário reexibido com os erros (HTTP 422);</li>
 *   <li>{@link ItemNaoEncontradoException} → HTTP 404;</li>
 *   <li>{@link RequisicaoInvalidaException} → HTTP 400;</li>
 *   <li>demais exceções (ex.: {@code PersistenciaException}) seguem para a página de
 *       erro global configurada no web.xml ({@link ErroServlet}).</li>
 * </ul>
 * Após gravar ou excluir, aplica o padrão Post/Redirect/Get, evitando reenvio do formulário.
 */
@WebServlet(name = "ItemServlet", urlPatterns = "/itens/*")
public class ItemServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ItemServlet.class.getName());

    private static final String VIEW_LISTA = "/WEB-INF/views/lista.jsp";
    private static final String VIEW_FORMULARIO = "/WEB-INF/views/formulario.jsp";
    private static final String FLASH = "mensagemFlash";
    private static final int SC_UNPROCESSABLE_ENTITY = 422;

    private transient ItemService servico;

    @Override
    public void init() throws ServletException {
        servico = (ItemService) getServletContext().getAttribute(InicializadorAplicacao.ATRIBUTO_SERVICO);
        if (servico == null) {
            throw new UnavailableException("O serviço do catálogo não foi inicializado.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (rota(req)) {
                case "/" -> listar(req, resp);
                case "/novo" -> exibirFormulario(req, resp, FormularioItem.vazio(), Map.of(),
                        HttpServletResponse.SC_OK);
                case "/editar" -> {
                    ItemCatalogo item = servico.buscarPorId(lerId(req));
                    exibirFormulario(req, resp, FormularioItem.doItem(item), Map.of(), HttpServletResponse.SC_OK);
                }
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Página não encontrada.");
            }
        } catch (ItemNaoEncontradoException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (RequisicaoInvalidaException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (rota(req)) {
                case "/salvar" -> salvar(req, resp);
                case "/excluir" -> excluir(req, resp);
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Página não encontrada.");
            }
        } catch (ItemNaoEncontradoException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (RequisicaoInvalidaException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void listar(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String busca = req.getParameter("busca");
        TipoMidia tipo = TipoMidia.deTexto(req.getParameter("tipo")).orElse(null);

        List<ItemCatalogo> itens = servico.listar(busca, tipo);
        Map<TipoMidia, Integer> resumo = servico.resumoPorTipo();

        req.setAttribute("itens", itens);
        req.setAttribute("resumo", resumo);
        req.setAttribute("totalGeral", resumo.values().stream().mapToInt(Integer::intValue).sum());
        req.setAttribute("busca", busca == null ? "" : busca.trim());
        req.setAttribute("tipoSelecionado", tipo);
        req.setAttribute("tipos", TipoMidia.values());
        moverMensagemFlash(req);
        req.getRequestDispatcher(VIEW_LISTA).forward(req, resp);
    }

    private void salvar(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String> form = FormularioItem.daRequisicao(req);
        boolean novo = form.get("id").isBlank();
        try {
            ItemCatalogo salvo = servico.salvarFormulario(form);
            definirMensagemFlash(req, (novo ? "Item cadastrado" : "Item atualizado")
                    + " com sucesso: " + salvo.getTitulo());
            resp.sendRedirect(req.getContextPath() + "/itens");
        } catch (ValidacaoException e) {
            LOG.fine(() -> "Formulário rejeitado: " + e.getMessage());
            exibirFormulario(req, resp, form, e.getErros(), SC_UNPROCESSABLE_ENTITY);
        }
    }

    private void excluir(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = lerId(req);
        ItemCatalogo item = servico.buscarPorId(id);
        servico.excluir(id);
        definirMensagemFlash(req, "Item excluído: " + item.getTitulo());
        resp.sendRedirect(req.getContextPath() + "/itens");
    }

    private void exibirFormulario(HttpServletRequest req, HttpServletResponse resp, Map<String, String> form,
                                  Map<String, String> erros, int status) throws ServletException, IOException {
        req.setAttribute("form", form);
        req.setAttribute("erros", erros);
        req.setAttribute("edicao", !form.getOrDefault("id", "").isBlank());
        req.setAttribute("tipos", TipoMidia.values());
        req.setAttribute("anoMinimo", servico.getValidador().getAnoMinimo());
        req.setAttribute("anoMaximo", servico.getValidador().getAnoMaximo());
        resp.setStatus(status);
        req.getRequestDispatcher(VIEW_FORMULARIO).forward(req, resp);
    }

    private static String rota(HttpServletRequest req) {
        String caminho = req.getPathInfo();
        return caminho == null || caminho.isEmpty() ? "/" : caminho;
    }

    /** Lê e valida o parâmetro "id"; nunca deixa um NumberFormatException escapar. */
    private static int lerId(HttpServletRequest req) {
        String valor = req.getParameter("id");
        if (valor == null || valor.isBlank()) {
            throw new RequisicaoInvalidaException("O identificador do item não foi informado.");
        }
        try {
            int id = Integer.parseInt(valor.trim());
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new RequisicaoInvalidaException("Identificador de item inválido: \"" + valor + "\".");
        }
    }

    private static void definirMensagemFlash(HttpServletRequest req, String mensagem) {
        req.getSession().setAttribute(FLASH, mensagem);
    }

    private static void moverMensagemFlash(HttpServletRequest req) {
        HttpSession sessao = req.getSession(false);
        if (sessao != null && sessao.getAttribute(FLASH) != null) {
            req.setAttribute(FLASH, sessao.getAttribute(FLASH));
            sessao.removeAttribute(FLASH);
        }
    }
}
