package org.example.web.tool;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    private static volatile Algorithm algorithm;

    public JwtUtil(@Value("${JWT_SECRET:}") String secret) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException("JWT_SECRET must be configured with at least 32 random bytes");
        algorithm = Algorithm.HMAC256(secret);
    }

    private static Algorithm configured() {
        if (algorithm == null) throw new IllegalStateException("JWT signing is not initialized");
        return algorithm;
    }

    public static String genToken(Map<String, Object> claims) {
        return "Bearer " + JWT.create().withClaim("claims", claims).withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 72))
            .sign(configured());
    }

    public static Map<String, Object> parseToken(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Missing token");
        if (token.startsWith("Bearer ")) token = token.substring(7);
        return JWT.require(configured()).build().verify(token).getClaim("claims").asMap();
    }
}
