package br.edu.uniesp.dearfuture.capsule;

import br.edu.uniesp.dearfuture.internal.capsule.controller.CapsuleCreationController;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleCreationService;
import br.edu.uniesp.dearfuture.internal.capsule.dto.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class CapsuleCreationControllerTest {
    @Test void delegatesWithAuthenticatedIdentity() throws Exception {
        var service = mock(CapsuleCreationService.class);
        var owner = UUID.randomUUID();
        var id = UUID.randomUUID();
        var now = Instant.parse("2030-01-01T12:00:00Z");
        when(service.create(eq(owner), any())).thenReturn(new CapsuleSummary(id, "T", now, now.plusSeconds(60), "SEALED"));
        var mvc = MockMvcBuilders.standaloneSetup(new CapsuleCreationController(service)).build();
        var principal = new UsernamePasswordAuthenticationToken(owner.toString(), "unused");
        mvc.perform(post("/api/v1/capsules").principal(principal).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"T\",\"message\":\"Carta\",\"opensAt\":\"2030-01-01T12:01:00Z\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/capsules/" + id)).andExpect(jsonPath("$.message").doesNotExist());
    }
}
