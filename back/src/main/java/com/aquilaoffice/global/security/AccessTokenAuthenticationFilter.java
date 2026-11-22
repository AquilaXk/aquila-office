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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer access token 또는 presence SSE 단기 티켓을 검증해 Spring Security 인증으로 변환한다.
 */
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Pattern PRESENCE_STREAM_PATH_PATTERN =
            Pattern.compile("^/api/v1/workspaces/(\\d+)/presence/stream$");

    private final BearerTokenResolver bearerTokenResolver;
    private final ResolveAccessTokenPort resolveAccessTokenPort;
    private final LoadMemberPort loadMemberPort;
    private final PresenceStreamTicketService presenceStreamTicketService;
    private final boolean publicDocsAndMetrics;

    public AccessTokenAuthenticationFilter(
            BearerTokenResolver bearerTokenResolver,
            ResolveAccessTokenPort resolveAccessTokenPort,
            LoadMemberPort loadMemberPort) {
        this(bearerTokenResolver, resolveAccessTokenPort, loadMemberPort, null, false);
    }

    public AccessTokenAuthenticationFilter(
            BearerTokenResolver bearerTokenResolver,
            ResolveAccessTokenPort resolveAccessTokenPort,
            LoadMemberPort loadMemberPort,
            PresenceStreamTicketService presenceStreamTicketService,
            boolean publicDocsAndMetrics) {
        this.bearerTokenResolver = bearerTokenResolver;
        this.resolveAccessTokenPort = resolveAccessTokenPort;
        this.loadMemberPort = loadMemberPort;
        this.presenceStreamTicketService = presenceStreamTicketService;
        this.publicDocsAndMetrics = publicDocsAndMetrics;
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
                || isPublicOpsOrDocsPath(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        try {
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                authenticateAccessToken(bearerTokenResolver.resolve(authorizationHeader));
                filterChain.doFilter(request, response);
                return;
            }

            Long presenceWorkspaceId = resolvePresenceStreamWorkspaceId(request);
            if (presenceWorkspaceId != null && tryAuthenticatePresenceStream(request, presenceWorkspaceId)) {
                filterChain.doFilter(request, response);
                return;
            }

            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private void authenticateAccessToken(String accessToken) {
        ResolvedAccessToken resolvedAccessToken = resolveAccessTokenPort.resolveAccessToken(accessToken);
        Member member = resolveMember(resolvedAccessToken)
                .orElseThrow(() -> new IllegalArgumentException("authenticated member is not found"));
        setAuthenticationContext(
                resolvedAccessToken.memberId() != null ? resolvedAccessToken.memberId() : member.getId(),
                member);
    }

    private boolean tryAuthenticatePresenceStream(HttpServletRequest request, Long workspaceId) {
        String ticket = request.getParameter("ticket");
        if (ticket != null && !ticket.isBlank()) {
            if (presenceStreamTicketService == null) {
                throw new IllegalArgumentException("presence stream ticket service unavailable");
            }
            PresenceStreamTicketService.PresenceTicketPrincipal principal = presenceStreamTicketService
                    .consumeTicket(ticket, workspaceId)
                    .orElseThrow(() -> new IllegalArgumentException("invalid or expired presence stream ticket"));
            Member member = loadMemberPort
                    .findByGoogleSub(principal.memberGoogleSub())
                    .orElseThrow(() -> new IllegalArgumentException("authenticated member is not found"));
            setAuthenticationContext(member.getId(), member);
            return true;
        }

        String accessTokenParam = firstNonBlank(
                request.getParameter("accessToken"),
                request.getParameter("token"));
        if (accessTokenParam != null) {
            authenticateAccessToken(accessTokenParam);
            return true;
        }
        return false;
    }

    private void setAuthenticationContext(Long memberId, Member member) {
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(
                memberId,
                member.getGoogleSub(),
                member.getEmail(),
                member.getRole());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authenticatedMember,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Long resolvePresenceStreamWorkspaceId(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return null;
        }
        String path = resolvePathWithinApplication(request);
        Matcher matcher = PRESENCE_STREAM_PATH_PATTERN.matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isPublicOpsOrDocsPath(String path) {
        if (!publicDocsAndMetrics) {
            return false;
        }
        return path.startsWith("/actuator/metrics")
                || path.startsWith("/actuator/prometheus")
                || path.startsWith("/v3/api-docs")
                || "/swagger-ui.html".equals(path)
                || path.startsWith("/swagger-ui/");
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

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
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
