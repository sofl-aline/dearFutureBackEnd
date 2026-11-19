package br.edu.uniesp.dearfuture.internal.auth.service;

import br.edu.uniesp.dearfuture.internal.auth.dto.*;
import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import static br.edu.uniesp.dearfuture.shared.exception.ApiException.Code.*;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authentication;
    private final JwtEncoder jwt;
    private final Clock clock;
    private final String issuer;
    private final String audience;
    private final long ttl;
    public AuthService(UserRepository users, PasswordEncoder encoder, AuthenticationManager authentication,
            JwtEncoder jwt, Clock clock, @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience, @Value("${app.jwt.ttl-seconds}") long ttl) {
        this.users = users; this.encoder = encoder; this.authentication = authentication;
        this.jwt = jwt; this.clock = clock; this.issuer = issuer; this.audience = audience; this.ttl = ttl;
    }
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (users.existsByEmail(request.email())) throw duplicateEmail();
        var user = new User(UUID.randomUUID(), request.name(), request.email(),
                encoder.encode(request.password()), clock.instant());
        try { users.saveAndFlush(user); }
        catch (DataIntegrityViolationException exception) {
            // A checagem anterior não impede dois cadastros simultâneos do mesmo e-mail.
            // Não continuar usando a transação após a violação.
            Throwable cause = exception;
            while (cause != null) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                        && violation.getConstraintName() != null
                        && violation.getConstraintName().toLowerCase(java.util.Locale.ROOT).contains("uk_users_email")) {
                    throw duplicateEmail();
                }
                cause = cause.getCause();
            }
            throw exception;
        }
        return UserResponse.from(user);
    }
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        try { authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password())); }
        catch (AuthenticationException exception) { throw new ApiException(INVALID_CREDENTIALS, "E-mail ou senha inválidos."); }
        var user = users.findByEmail(request.email()).orElseThrow(() -> new ApiException(INVALID_CREDENTIALS, "E-mail ou senha inválidos."));
        var now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(user.getId().toString())
                .audience(List.of(audience)).issuedAt(now).expiresAt(now.plusSeconds(ttl)).build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return new TokenResponse(jwt.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), "Bearer", ttl);
    }
    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        return users.findById(userId).map(UserResponse::from)
                .orElseThrow(() -> new ApiException(UNAUTHENTICATED, "Usuário não encontrado."));
    }
    private ApiException duplicateEmail() { return new ApiException(EMAIL_ALREADY_REGISTERED, "E-mail já cadastrado."); }
}
