package com.aquilaoffice.global.security;

import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.ResolveAccessTokenPort;
import com.aquilaoffice.application.port.out.ResolvedAccessToken;
import com.aquilaoffice.domain.member.Member;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
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
    private final LoadMemberPort loadMemberPort;

    public AccessTokenAuthenticationFilter(
            BearerTokenResolver bearerTokenResolver,
            ResolveAccessTokenPort resolveAccessTokenPort,
            LoadMemberPort loadMemberPort) {
        this.bearerTokenResolver = bearerTokenResolver;
        this.resolveAccessTokenPort = resolveAccessTokenPort;
        this.loadMemberPort = loadMemberPort;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = resolvePathWithinApplication(request);
        return isPublicPostEndpoint(request, path)
                || isPublicGetEndpoint(request, path)
                || path.startsWith("/actuator/health")
                || path.startsWith("/actuator/metrics")
                || path.startsWith("/actuator/prometheus")
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
            Member member = resolveMember(resolvedAccessToken)
                    .orElseThrow(() -> new IllegalArgumentException("authenticated member is not found"));
            AuthenticatedMember authenticatedMember = new AuthenticatedMember(
                    resolvedAccessToken.memberId(),
                    member.getGoogleSub(),
                    member.getEmail(),
                    member.getRole());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedMember,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private Optional<Member> resolveMember(ResolvedAccessToken resolvedAccessToken) {
        if (resolvedAccessToken.googleSub() != null) {
            return loadMemberPort.findByGoogleSub(resolvedAccessToken.googleSub());
        }
        return loadMemberPort.findByEmail(resolvedAccessToken.email());
    }

    private boolean isPublicPostEndpoint(HttpServletRequest request, String path) {
        return HttpMethod.POST.matches(request.getMethod())
                && ("/api/v1/auth/google/login".equals(path)
                        || "/api/v1/auth/token/refresh".equals(path)
                        || "/api/v1/auth/logout".equals(path)
                        || "/api/v1/slack/events".equals(path));
    }

    private boolean isPublicGetEndpoint(HttpServletRequest request, String path) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        return "/api/health".equals(path)
                || "/api/v1/health".equals(path)
                || isSingleSegmentPath(path, "/api/v1/invites/")
                || isSingleSegmentPath(path, "/api/v1/workspace-invites/");
    }

    private static boolean isSingleSegmentPath(String path, String prefix) {
        if (!path.startsWith(prefix) || path.length() <= prefix.length()) {
            return false;
        }
        return path.indexOf('/', prefix.length()) < 0;
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
