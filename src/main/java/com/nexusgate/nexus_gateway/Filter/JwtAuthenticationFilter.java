package com.nexusgate.nexus_gateway.Filter;

import com.nexusgate.nexus_gateway.Config.Security.JwtTokenVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import java.util.List;
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtTokenVerifier tokenVerifier;

    public JwtAuthenticationFilter(JwtTokenVerifier tokenVerifier) {
        super(Config.class);
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {
            System.out.println(
                    "JWT FILTER HIT: "
                            + exchange.getRequest().getMethod()
                            + " "
                            + exchange.getRequest().getURI()
            );

            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst("Authorization");
            System.out.println(
                    "Authorization header present: "
                            + (authHeader != null)
            );

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse()
                        .setStatusCode(HttpStatus.UNAUTHORIZED);

                System.out.println("Invalid or missing Authorization header");
                return exchange.getResponse().setComplete();
            }

            String jwtToken = authHeader.substring(7);
            try {
                Claims claims = tokenVerifier.verify(jwtToken);

                // Authentication will be established here.
                String subject = claims.getSubject();
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(
                                subject,
                                null,
                                List.of()
                        );

                return chain.filter(exchange)
                        .contextWrite(
                                ReactiveSecurityContextHolder.withAuthentication(
                                        authentication
                                )
                        );
            } catch (JwtException | IllegalArgumentException e) {

                exchange.getResponse()
                        .setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    @Getter
    @Setter
    public static class Config {
    }
}
