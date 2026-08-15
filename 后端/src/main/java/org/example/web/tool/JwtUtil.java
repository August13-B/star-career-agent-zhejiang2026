package org.example.web.tool;

import java.util.Date;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

public class JwtUtil {

    private static final String KEY = "August";

    //接收业务数据,生成token并返回
    public static String genToken(Map<String, Object> claims) {
        String token = JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 72))
                .sign(Algorithm.HMAC256(KEY));
        return "Bearer " + token;
    }

    //接收token,验证token,并返回业务数据
    public static Map<String, Object> parseToken(String token) {
        // 去除可能的Bearer前缀
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return JWT.require(Algorithm.HMAC256(KEY))
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();
    }
}
