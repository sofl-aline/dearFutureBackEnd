package br.edu.uniesp.dearfuture.capsule;

import br.edu.uniesp.dearfuture.internal.capsule.controller.CapsuleQueryController;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleQueryService;
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
class CapsuleQueryControllerTest {
    @Test void delegatesWithAuthenticatedIdentity() throws Exception {
        var service = mock(CapsuleQueryService.class);
        var owner = UUID.randomUUID();
        var id = UUID.randomUUID();
        var now = Instant.parse("2030-01-01T12:00:00Z");
        when(service.get(owner, id)).thenReturn(new CapsuleSummary(id, "T", now, now.plusSeconds(60), "SEALED"));
        var mvc = MockMvcBuilders.standaloneSetup(new CapsuleQueryController(service)).build();
        var principal = new UsernamePasswordAuthenticationToken(owner.toString(), "unused");
        mvc.perform(get("/api/v1/capsules/" + id).principal(principal))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").doesNotExist());
    }
}
