package br.edu.uniesp.dearfuture.config;

import br.edu.uniesp.dearfuture.config.exception.GlobalExceptionHandler;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class GlobalExceptionHandlerTest {
    @RestController static class FixtureController {
        @GetMapping("/fixture/missing") String missing() { throw new ApiException(ApiException.Code.CAPSULE_NOT_FOUND, "Cápsula não encontrada."); }
        @GetMapping("/fixture/sealed") String sealed() { throw new ApiException(ApiException.Code.CAPSULE_SEALED, "A cápsula ainda está selada."); }
        @GetMapping("/fixture/bug") String bug() { throw new IllegalStateException("NAO_EXPOR_CARTA"); }
    }
    @Test void translatesErrorsWithoutExposingUnexpectedDetails() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FixtureController())
                .setControllerAdvice(new GlobalExceptionHandler(new ProblemResponses(JsonMapper.builder().build()))).build();
        mvc.perform(get("/fixture/missing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAPSULE_NOT_FOUND"));
        mvc.perform(get("/fixture/sealed")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CAPSULE_SEALED"));
        mvc.perform(get("/fixture/bug")).andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("NAO_EXPOR_CARTA"))));
    }
}