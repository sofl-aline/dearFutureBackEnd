package br.edu.uniesp.dearfuture.auth;

import br.edu.uniesp.dearfuture.internal.auth.dto.*;
import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import br.edu.uniesp.dearfuture.internal.auth.service.AuthService;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuthServiceTest {
    final UserRepository users = mock(UserRepository.class);
    final PasswordEncoder encoder = mock(PasswordEncoder.class);
    final AuthenticationManager manager = mock(AuthenticationManager.class);
    final JwtEncoder jwt = mock(JwtEncoder.class);
    final Clock clock = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneOffset.UTC);
    final AuthService service = new AuthService(users, encoder, manager, jwt, clock, "dearfuture", "dearfuture-api", 900);
    @Test void normalizesEmailAndStoresEncodedPassword() {
        when(encoder.encode("senha-longa-de-teste")).thenReturn("hash");
        var result = service.register(new RegisterRequest(" Ana ", " ANA@EXAMPLE.COM ", "senha-longa-de-teste"));
        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(result.email()).isEqualTo("ana@example.com");
        assertThat(result.name()).isEqualTo("Ana");
    }
    @Test void rejectsDuplicateEmailWithoutSaving() {
        when(users.existsByEmail("ana@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("Ana", "ana@example.com", "senha-longa-de-teste")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(ApiException.Code.EMAIL_ALREADY_REGISTERED));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void concurrentDuplicateIsTranslated() {
        when(encoder.encode(anyString())).thenReturn("hash");
        when(users.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("fixture",
                new org.hibernate.exception.ConstraintViolationException("fixture", new java.sql.SQLException("fixture"), "uk_users_email")));
        assertThatThrownBy(() -> service.register(new RegisterRequest("Ana", "ana@example.com", "senha-longa-de-teste")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(ApiException.Code.EMAIL_ALREADY_REGISTERED));
    }
    @Test void invalidCredentialsDoNotIssueToken() {
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("fixture"));
        assertThatThrownBy(() -> service.login(new LoginRequest("ana@example.com", "senha-longa-de-teste")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(ApiException.Code.INVALID_CREDENTIALS));
        verifyNoInteractions(jwt);
    }
}
