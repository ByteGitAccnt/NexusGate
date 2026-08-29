package com.nexusgate.nexus_gateway.Config.Security;

import lombok.Data;

@Data
public class JwtConfig {

    private boolean enabled;
    private String algorithm;
    private JwtVerificationConfig verification;
    private String issuer;
    private String audience;

}
