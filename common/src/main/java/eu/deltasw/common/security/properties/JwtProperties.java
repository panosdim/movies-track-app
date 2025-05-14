package eu.deltasw.common.security.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long expirationTimeMs = 36000000; // 10 hours in milliseconds
}