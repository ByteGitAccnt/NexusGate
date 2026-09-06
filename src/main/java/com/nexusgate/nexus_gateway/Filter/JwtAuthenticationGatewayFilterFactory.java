package com.nexusgate.nexus_gateway.Filter;

import com.nexusgate.nexus_gateway.Config.Security.JwtTokenVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import java.util.List;

@Component
public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private final JwtTokenVerifier tokenVerifier;

    public JwtAuthenticationGatewayFilterFactory(JwtTokenVerifier tokenVerifier) {
        super(Config.class);
        this.tokenVerifier = tokenVerifier;

        System.out.println(
                "JWT FACTORY CREATED: " + this.getClass().getName()
        );
    }

    @Override
    public GatewayFilter apply(Config config) {

        System.out.println(
                "CONFIG CLASS: " + config.getClass().getName()
        );

        System.out.println(
                "CONFIG OBJECT: " + config
        );
        return (exchange, chain) -> {
            System.out.println(
                    "JWT FILTER HIT: "
                            + exchange.getRequest().getMethod()
                            + " "
                            + exchange.getRequest().getURI()
            );

            if(config.getPublicEndpoints() == null){
                System.out.println("publicEndpoints from apllay  is null");
            }

            String requestPath = exchange.getRequest().getPath().value();
            List<String> publicEndpoints =
                    config.getPublicEndpoints() == null
                            ? List.of()
                            : config.getPublicEndpoints();
            String relativePath = requestPath.startsWith(config.getServicePath())
                    ? requestPath.substring(config.getServicePath().length())
                    : requestPath;

            boolean isPublic = publicEndpoints.stream()
                    .anyMatch(relativePath::equals);

            System.out.println("Request path: " + requestPath);
            System.out.println("Relative path: " + relativePath);
            System.out.println("Public endpoints: " + publicEndpoints);
            System.out.println("Is public: " + isPublic);

            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst("Authorization");

            /*System.out.println(
                    "Authorization header present: "
                            + (authHeader != null)
            );*/
            //skips the auth verification
            if (isPublic && authHeader == null) {
                System.out.println("Authorization header is null");
                return chain.filter(exchange);
            }

            if ( authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

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
        private List<String> publicEndpoints;
        private String servicePath;
    }
}
