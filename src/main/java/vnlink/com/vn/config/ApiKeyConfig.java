package vnlink.com.vn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Component
@ConfigurationProperties(prefix = "api.key")
public class ApiKeyConfig {
    
    @Value("${api.key.header:API-KEY}")
    private String apiKeyHeader;
    
    private Map<String, String> clients = new HashMap<>();
    private final Map<String, String> validApiKeys = new HashMap<>();
    
    @PostConstruct
    public void init() {
        // Chuyển đổi clients từ YAML thành validApiKeys
        validApiKeys.putAll(clients);
    }
    
    public void setClients(Map<String, String> clients) {
        this.clients = clients;
    }
    
    public String getApiKeyHeader() {
        return apiKeyHeader;
    }
    
    public boolean isValidApiKey(String apiKey) {
        return apiKey != null && validApiKeys.containsKey(apiKey);
    }
    
    public String getClientId(String apiKey) {
        return validApiKeys.get(apiKey);
    }
} 