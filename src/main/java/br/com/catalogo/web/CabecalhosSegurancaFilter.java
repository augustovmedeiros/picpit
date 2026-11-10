package br.com.catalogo.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Adiciona cabeçalhos HTTP de segurança a todas as respostas.
 * <p>
 * A Content-Security-Policy só permite scripts e estilos do próprio site (e as
 * fontes do Google Fonts), reduzindo o impacto de um eventual XSS. Os dados
 * exibidos nas JSPs também são escapados com {@code <c:out>}.
 */
@WebFilter(filterName = "CabecalhosSegurancaFilter", urlPatterns = "/*")
public class CabecalhosSegurancaFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;

    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com",
            "img-src 'self' data:",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        resp.setHeader("Content-Security-Policy", CSP);
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "same-origin");
        chain.doFilter(req, resp);
    }
}
