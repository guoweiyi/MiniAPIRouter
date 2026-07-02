package com.miniapi.router.saas.security;

import com.miniapi.router.saas.context.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && tokenProvider.validateToken(token)) {
            Claims claims = tokenProvider.parseToken(token);
            String role = claims.get("role", String.class);
            Long tenantId = claims.get("tenant_id", Long.class);
            if (tenantId == null) {
                Object tid = claims.get("tenant_id");
                if (tid instanceof Number n) tenantId = n.longValue();
            }
            Long userId = Long.parseLong(claims.getSubject());
            String username = claims.get("username", String.class);

            TenantContext.setTenantId(tenantId);
            TenantContext.setUserId(userId);
            TenantContext.setRole(role);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (!token.startsWith("sk-miniapi")) {
                return token;
            }
        }
        return null;
    }
}
