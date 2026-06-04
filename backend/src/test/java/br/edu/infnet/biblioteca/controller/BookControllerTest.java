package br.edu.infnet.biblioteca.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.edu.infnet.biblioteca.model.dto.BookRequest;
import br.edu.infnet.biblioteca.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class BookControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        repository.deleteAll();
    }

    @Test
    void postCreatesBookAndReturns201WithLocation() throws Exception {
        BookRequest req = new BookRequest("Clean Code", "Robert C. Martin", "978-0-13-235088-4", 2008);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.isbn").value("978-0-13-235088-4"));
    }

    @Test
    void postWithDuplicatedIsbnReturns409() throws Exception {
        BookRequest req = new BookRequest("Clean Code", "Robert C. Martin", "978-0-13-235088-4", 2008);
        String body = objectMapper.writeValueAsString(req);

        mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate_isbn"));
    }

    @Test
    void postWithInvalidBodyReturns400WithFieldErrors() throws Exception {
        String invalid = """
                {"title": "", "author": "", "isbn": "??", "publishedYear": 1000}
                """;

        mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_error"))
                .andExpect(jsonPath("$.fields").isArray());
    }

    @Test
    void getUnknownIdReturns404() throws Exception {
        mockMvc.perform(get("/api/books/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("book_not_found"));
    }

    @Test
    void fullCrudFlow() throws Exception {
        BookRequest createReq = new BookRequest("DDD", "Eric Evans", "978-0-321-12521-7", 2003);
        String created = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        BookRequest updateReq = new BookRequest("DDD (rev)", "Eric Evans", "978-0-321-12521-7", 2003);
        mockMvc.perform(put("/api/books/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("DDD (rev)"));

        mockMvc.perform(delete("/api/books/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isNotFound());
    }
}
