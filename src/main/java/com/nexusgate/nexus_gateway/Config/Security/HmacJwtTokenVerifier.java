package com.nexusgate.nexus_gateway.Config.Security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class HmacJwtTokenVerifier implements JwtTokenVerifier {

    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public HmacJwtTokenVerifier(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;

        this.secretKey = Keys.hmacShaKeyFor(
                jwtConfig.getVerification()
                        .getSecret()
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public Claims verify(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtConfig.getIssuer())
                .requireAudience(jwtConfig.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
