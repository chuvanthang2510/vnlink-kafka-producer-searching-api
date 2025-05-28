package vnlink.com.vn.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import vnlink.com.vn.config.ApiKeyConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final ApiKeyConfig apiKeyConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiKey = request.getHeader(apiKeyConfig.getApiKeyHeader());
        
        if (!apiKeyConfig.isValidApiKey(apiKey)) {
            log.warn("Invalid API key attempt from IP: {}", request.getRemoteAddr());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid API key");
            return false;
        }
        
        String clientId = apiKeyConfig.getClientId(apiKey);
        log.info("Valid API key request from client: {}", clientId);
        return true;
    }
} 