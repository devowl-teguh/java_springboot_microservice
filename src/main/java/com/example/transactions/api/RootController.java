package com.example.transactions.api;

import io.swagger.v3.oas.annotations.Hidden;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The API has no home page; send visitors of the bare URL (for example from a CV link) to the interactive docs.
 * Disabled together with Swagger UI (API_DOCS_ENABLED=false), so the root then returns the usual 404 ProblemDetail.
 */
@Hidden
@RestController
@ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", havingValue = "true", matchIfMissing = true)
public class RootController {
    static final String DOCS = "/swagger-ui/index.html";

    @GetMapping("/")
    ResponseEntity<Void> root() {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(DOCS)).build();
    }
}
