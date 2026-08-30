package com.nexusgate.nexus_gateway.Config.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.security.Keys;


@Component
public class HmacJwtTokenVerifier implements JwtTokenVerifier {

    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public HmacJwtTokenVerifier(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;

        String secret = jwtConfig.getVerification().getSecret();

        /*this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );*/
        this.secretKey = createSecretKey(
                secret,
                jwtConfig.getAlgorithm()
        );
        System.out.println("JWT secret resolved: " + (secret != null));
        System.out.println("Secret length: " + secret.length());
        System.out.println("Configured algorithm: " + jwtConfig.getAlgorithm());
        System.out.println("Key algorithm: " + secretKey.getAlgorithm());
        /*this.jwtConfig = jwtConfig;
        String secret = jwtConfig.getVerification().getSecret();
        this.secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );*/
        /*this.secretKey = createSecretKey(
                secret,
                jwtConfig.getAlgorithm()
        );*/
    }

    @Override
    public Claims verify(String token) {
        try {
            /*String tokenAlgorithm = Jwts.parser()
                    .build()
                    .parse(token)
                    .getHeader()
                    .getAlgorithm();

            String configuredAlgorithm = jwtConfig.getAlgorithm();

            if (!configuredAlgorithm.equals(tokenAlgorithm)) {
                throw new JwtException(
                        "JWT algorithm does not match configured algorithm"
                );
            }*/
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(jwtConfig.getIssuer())
                    .requireAudience(jwtConfig.getAudience())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (JwtException | IllegalArgumentException e) {

            System.out.println(
                    "JWT ERROR TYPE: " + e.getClass().getName()
            );
            System.out.println(
                    "JWT ERROR: " + e.getMessage()
            );
            throw e;
        }
    }

    private SecretKey createSecretKey(String secret, String algorithm) {

        String keyAlgorithm = switch (algorithm) {
            case "HS256" -> "HmacSHA256";
            case "HS384" -> "HmacSHA384";
            case "HS512" -> "HmacSHA512";
            default -> throw new IllegalArgumentException(
                    "Unsupported HMAC algorithm: " + algorithm
            );
        };

        return new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                keyAlgorithm
        );
    }
}
