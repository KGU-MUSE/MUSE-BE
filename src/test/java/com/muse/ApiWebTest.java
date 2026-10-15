package com.muse;

import com.muse.global.config.SecurityConfig;
import com.muse.global.exception.GlobalExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiWebTest.ProbeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiWebTest.ProbeController.class})
class ApiWebTest {
    @Autowired MockMvc mvc;

    @Test
    void anonymousRequestsAreRejectedWithJson() throws Exception {
        mvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void authenticatedRequestsRemainDeniedUntilAuthenticationIsImplemented() throws Exception {
        mvc.perform(get("/api/projects").with(user("tester")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void corsAllowsTheConfiguredFrontend() throws Exception {
        mvc.perform(options("/api/projects").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsRejectsUnknownOrigins() throws Exception {
        mvc.perform(options("/api/projects").header("Origin", "https://unknown.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unexpectedErrorsDoNotExposeInternalMessages() throws Exception {
        mvc.perform(get("/api/health").param("failure", "unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    void httpErrorsKeepTheirStatus() throws Exception {
        mvc.perform(get("/api/health").param("failure", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidRequestBodiesReturnBadRequest() throws Exception {
        mvc.perform(post("/api/health").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/health").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @RestController
    static class ProbeController {
        @GetMapping("/api/health")
        String probe(@RequestParam String failure) {
            if (failure.equals("missing")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "찾을 수 없습니다.");
            }
            throw new IllegalStateException("secret database details");
        }

        @PostMapping("/api/health")
        void validate(@Valid @RequestBody ProbeRequest request) {}
    }

    record ProbeRequest(@NotBlank String name) {}
}
