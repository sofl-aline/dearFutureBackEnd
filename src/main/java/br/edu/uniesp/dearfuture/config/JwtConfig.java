package br.edu.uniesp.dearfuture.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
@Configuration
public class JwtConfig {
    @Bean public RSAPublicKey publicKey(@Value("${app.jwt.public-key}") Resource resource) throws IOException {
        try (var input = resource.getInputStream()) { return RsaKeyConverters.x509().convert(input); }
    }
    @Bean public RSAPrivateKey privateKey(@Value("${app.jwt.private-key}") Resource resource) throws IOException {
        try (var input = resource.getInputStream()) { return RsaKeyConverters.pkcs8().convert(input); }
    }
    @Bean public JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        RSAKey key = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("dearfuture-local").build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
    @Bean public JwtDecoder jwtDecoder(RSAPublicKey publicKey,
            @Value("${app.jwt.issuer}") String issuer, @Value("${app.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        OAuth2TokenValidator<Jwt> subjectValidator = jwt -> {
            try { java.util.UUID.fromString(jwt.getSubject()); return OAuth2TokenValidatorResult.success(); }
            catch (IllegalArgumentException | NullPointerException e) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audienceValidator, subjectValidator));
        return decoder;
    }
}
