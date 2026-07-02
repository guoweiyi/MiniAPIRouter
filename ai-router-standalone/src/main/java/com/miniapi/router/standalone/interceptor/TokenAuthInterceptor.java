package com.miniapi.router.standalone.interceptor;

import com.miniapi.router.core.config.CoreProperties;
import com.miniapi.router.core.exception.RouterException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenAuthInterceptor implements HandlerInterceptor {

    private final String authToken;

    public TokenAuthInterceptor(CoreProperties properties) {
        this.authToken = properties.getAuthToken();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            if (authToken.equals(token)) return true;
        }
        String xApiKey = request.getHeader("x-api-key");
        if (authToken.equals(xApiKey)) return true;
        throw new RouterException("UNAUTHORIZED", "Invalid or missing token", 401);
    }
}
