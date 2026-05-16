package com.aquilaoffice.global.security;

import com.aquilaoffice.application.port.out.ResolveAccessTokenPort;
import com.aquilaoffice.application.port.out.ResolvedAccessToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer access token을 검증해 Spring Security 인증으로 변환하는 filter다.
 */
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {

    private final BearerTokenResolver bearerTokenResolver;
    private final ResolveAccessTokenPort resolveAccessTokenPort;

    public AccessTokenAuthenticationFilter(
            BearerTokenResolver bearerTokenResolver,
            ResolveAccessTokenPort resolveAccessTokenPort) {
        this.bearerTokenResolver = bearerTokenResolver;
        this.resolveAccessTokenPort = resolveAccessTokenPort;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = resolvePathWithinApplication(request);
        return isPublicPostEndpoint(request, path)
                || "/api/health".equals(path)
                || path.startsWith("/actuator/health")
                || path.startsWith("/v3/api-docs")
                || "/swagger-ui.html".equals(path)
                || path.startsWith("/swagger-ui/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String accessToken = bearerTokenResolver.resolve(authorizationHeader);
            ResolvedAccessToken resolvedAccessToken = resolveAccessTokenPort.resolveAccessToken(accessToken);
            AuthenticatedMember authenticatedMember = new AuthenticatedMember(
                    resolvedAccessToken.memberId(),
                    resolvedAccessToken.email(),
                    resolvedAccessToken.role());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedMember,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + resolvedAccessToken.role().name())));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private boolean isPublicPostEndpoint(HttpServletRequest request, String path) {
        return HttpMethod.POST.matches(request.getMethod())
                && ("/api/v1/auth/google/login".equals(path)
                        || "/api/v1/auth/token/refresh".equals(path)
                        || "/api/v1/auth/logout".equals(path));
    }

    private String resolvePathWithinApplication(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isBlank()) {
            return servletPath;
        }

        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank()) {
            return "";
        }

        String contextPath = request.getContextPath();
        if (contextPath == null || contextPath.isBlank()) {
            return requestUri;
        }

        if (requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
