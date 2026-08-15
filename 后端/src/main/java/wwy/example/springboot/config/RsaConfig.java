package wwy.example.springboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component("jobRsaConfig")
@ConfigurationProperties(prefix = "rsa")
public class RsaConfig {
    private String privateKey;
    private String publicKey;
}