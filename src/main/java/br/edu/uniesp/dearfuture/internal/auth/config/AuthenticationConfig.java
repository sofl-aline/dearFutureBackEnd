package br.edu.uniesp.dearfuture.internal.auth.config;

import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class AuthenticationConfig {
    @Bean public UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(user -> User.withUsername(user.getEmail()).password(user.getPasswordHash())
                        .authorities("USER").build())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }
    @Bean public AuthenticationManager authenticationManager(UserDetailsService details, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(details);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}
