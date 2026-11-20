package com.example.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.transactions.api.OpenApiConfiguration;
import com.example.transactions.api.MemberController;
import com.example.transactions.api.TransactionController;
import com.example.transactions.domain.MemberService;
import com.example.transactions.domain.TransactionService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Keeps the committed spec at docs/openapi.yaml in sync with the code, so it can be imported into Postman,
 * Insomnia, Bruno, etc. without running the app. If the API changes, this test rewrites the file and fails once;
 * review and commit the updated file, then rerun.
 */
// Pin the server URL so a local .env or OPENAPI_SERVER_URL cannot change the generated spec.
@WebMvcTest(controllers = {TransactionController.class, MemberController.class}, properties = "app.openapi.server-url=http://localhost:8080")
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class,
        SpringDocWebMvcConfiguration.class, MultipleOpenApiSupportConfiguration.class})
@Import(OpenApiConfiguration.class)
class OpenApiSpecTest {
    private static final Path SPEC = Path.of("docs/openapi.yaml");

    @Autowired MockMvc mvc;
    @MockitoBean TransactionService service;
    @MockitoBean MemberService members;

    @Test void committedSpecMatchesGeneratedSpec() throws Exception {
        String generated = mvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(generated).contains("/transactions/{id}", "createTransaction", "getTransaction");

        String committed = Files.exists(SPEC) ? Files.readString(SPEC) : "";
        if (!normalize(committed).equals(normalize(generated))) {
            Files.createDirectories(SPEC.getParent());
            Files.writeString(SPEC, generated);
        }
        assertThat(normalize(committed))
                .as("docs/openapi.yaml was out of date and has been regenerated; review and commit it")
                .isEqualTo(normalize(generated));
    }

    private static String normalize(String s) { return s.replace("\r\n", "\n").strip(); }
}
