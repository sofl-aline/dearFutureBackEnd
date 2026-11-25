package br.edu.uniesp.dearfuture.integration;

import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompleteFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired CapsuleRepository capsules;
    @Autowired JwtEncoder encoder;
    @MockitoBean Clock clock;
    Instant now;
    @BeforeEach void setup() {
        capsules.deleteAll(); users.deleteAll();
        now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        when(clock.instant()).thenReturn(now);
    }
    String account(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "Pessoa", "email", email, "password", "senha-longa-de-teste"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(users.findByEmail(email).orElseThrow().getPasswordHash()).startsWith("{pbkdf2}").isNotEqualTo("senha-longa-de-teste");
        var result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "senha-longa-de-teste"))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
    }
    String create(String token) throws Exception {
        var input = Map.of("title", "T", "message", "Carta secreta", "opensAt", now.plusSeconds(120).toString());
        var result = mvc.perform(post("/api/v1/capsules").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store")).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }
    @Test void isolatesAccountsAndChecksExactOpeningTime() throws Exception {
        String a = account("a@example.com"), b = account("b@example.com");
        String id = create(a);
        mvc.perform(get("/api/v1/capsules").header("Authorization", "Bearer " + a))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id)).andExpect(jsonPath("$[0].message").doesNotExist());
        mvc.perform(get("/api/v1/capsules").header("Authorization", "Bearer " + b)).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/v1/capsules/" + id).header("Authorization", "Bearer " + b)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/capsules/" + id + "/message").header("Authorization", "Bearer " + b))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAPSULE_NOT_FOUND"));
        mvc.perform(get("/api/v1/capsules/" + id + "/message").header("Authorization", "Bearer " + a))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CAPSULE_SEALED"));
        when(clock.instant()).thenReturn(now.plusSeconds(120));
        mvc.perform(get("/api/v1/capsules/" + id + "/message").header("Authorization", "Bearer " + a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Carta secreta"));
        mvc.perform(get("/api/v1/capsules/" + id + "/message").header("Authorization", "Bearer " + b)).andExpect(status().isNotFound());
    }
    @Test void duplicateAndInvalidCredentialsHavePredictableErrors() throws Exception {
        account("a@example.com");
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "A", "email", "a@example.com", "password", "senha-longa-de-teste"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        for (String email : List.of("a@example.com", "missing@example.com")) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("email", email, "password", "senha-incorreta-longa"))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
        }
    }
    @Test void invalidJsonUuidAndDateWithoutOffsetAreRejected() throws Exception {
        String token = account("a@example.com");
        for (String date : List.of("2030-01-01T00:00:00", "invalid")) {
            mvc.perform(post("/api/v1/capsules").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("title", "T", "message", "Carta", "opensAt", date))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        mvc.perform(post("/api/v1/capsules").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/capsules").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title", "T", "message", "Carta", "opensAt", now.plusSeconds(120).toString(), "ownerId", UUID.randomUUID().toString()))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/capsules/not-uuid").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
    @Test void rejectsExpiredWrongAudienceAndMalformedTokens() throws Exception {
        mvc.perform(get("/api/v1/capsules")).andExpect(status().isUnauthorized());
        for (boolean expired : List.of(true, false)) {
            var claims = JwtClaimsSet.builder().issuer("dearfuture").subject(UUID.randomUUID().toString())
                    .audience(List.of(expired ? "dearfuture-api" : "wrong"))
                    .issuedAt(now.minusSeconds(7200)).expiresAt(expired ? now.minusSeconds(3600) : now.plusSeconds(900)).build();
            String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
            mvc.perform(get("/api/v1/capsules").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }
        mvc.perform(get("/api/v1/capsules").header("Authorization", "Bearer broken"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
    }
    @Test void preflightAndErrorsAllowFrontendOrigin() throws Exception {
        mvc.perform(options("/api/v1/capsules").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(get("/api/v1/capsules").header("Origin", "http://localhost:5173"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
