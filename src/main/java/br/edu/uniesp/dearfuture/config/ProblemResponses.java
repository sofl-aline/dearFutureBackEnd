package br.edu.uniesp.dearfuture.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
@Component
public class ProblemResponses {
    private final ObjectMapper mapper;
    public ProblemResponses(ObjectMapper mapper) { this.mapper = mapper; }
    public ProblemDetail create(HttpStatus status, String code, String detail, String path,
                                Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(path));
        problem.setProperty("code", code);
        problem.setProperty("erros", errors);
        return problem;
    }
    public void write(HttpServletResponse response, HttpStatus status, String code,
                      String detail, String path) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", path);
        body.put("code", code);
        body.put("erros", Map.of());
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getOutputStream(), body);
    }
}
