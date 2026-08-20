package br.edu.infnet.loans.controller;

import br.edu.infnet.loans.config.GlobalExceptionHandler;
import br.edu.infnet.loans.dto.CreateLoanRequest;
import br.edu.infnet.loans.dto.LoanResponse;
import br.edu.infnet.loans.exception.ActiveLoanExistsException;
import br.edu.infnet.loans.exception.CatalogBookNotFoundException;
import br.edu.infnet.loans.exception.CatalogUnavailableException;
import br.edu.infnet.loans.exception.LoanAlreadyReturnedException;
import br.edu.infnet.loans.exception.LoanNotFoundException;
import br.edu.infnet.loans.model.LoanStatus;
import br.edu.infnet.loans.service.LoanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanControllerTest {

    private LoanService service;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(LoanService.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new LoanController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listsLoansNewestFirst() throws Exception {
        when(service.list()).thenReturn(List.of(response(2L, LoanStatus.ACTIVE), response(1L, LoanStatus.RETURNED)));

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].id").value(1));
    }

    @Test
    void getsOneLoan() throws Exception {
        when(service.get(2L)).thenReturn(response(2L, LoanStatus.ACTIVE));

        mockMvc.perform(get("/api/loans/{id}", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createsALoanAndReturnsItsLocation() throws Exception {
        CreateLoanRequest request = new CreateLoanRequest(7L, "Maria Silva", LocalDate.parse("2100-01-15"));
        when(service.create(request)).thenReturn(response(41L, LoanStatus.ACTIVE));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/loans/41"))
                .andExpect(jsonPath("$.id").value(41));
    }

    @Test
    void returnsAnActiveLoan() throws Exception {
        when(service.returnLoan(41L)).thenReturn(response(41L, LoanStatus.RETURNED));

        mockMvc.perform(patch("/api/loans/{id}/return", 41))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
    }

    @Test
    void rejectsAnInvalidCreateRequestWithFieldDetails() throws Exception {
        String invalid = """
                {"bookId": 0, "borrowerName": "", "dueDate": "2020-01-01"}
                """;

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_error"))
                .andExpect(jsonPath("$.fields.length()").value(3));
    }

    @Test
    void mapsMissingLoansToNotFound() throws Exception {
        when(service.get(99L)).thenThrow(new LoanNotFoundException(99L));

        mockMvc.perform(get("/api/loans/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("loan_not_found"));
    }

    @Test
    void mapsMissingCatalogBooksToNotFound() throws Exception {
        CreateLoanRequest request = new CreateLoanRequest(99L, "Maria Silva", LocalDate.parse("2100-01-15"));
        when(service.create(request)).thenThrow(new CatalogBookNotFoundException(99L));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("catalog_book_not_found"));
    }

    @Test
    void mapsLoanConflictsToConflict() throws Exception {
        CreateLoanRequest request = new CreateLoanRequest(7L, "Maria Silva", LocalDate.parse("2100-01-15"));
        when(service.create(request)).thenThrow(new ActiveLoanExistsException(7L));
        when(service.returnLoan(41L)).thenThrow(new LoanAlreadyReturnedException(41L));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("active_loan_exists"));

        mockMvc.perform(patch("/api/loans/{id}/return", 41))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("loan_already_returned"));
    }

    @Test
    void mapsCatalogOutagesToServiceUnavailable() throws Exception {
        CreateLoanRequest request = new CreateLoanRequest(7L, "Maria Silva", LocalDate.parse("2100-01-15"));
        when(service.create(request)).thenThrow(new CatalogUnavailableException());

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("catalog_unavailable"));
    }

    private static LoanResponse response(Long id, LoanStatus status) {
        Instant returnedAt = status == LoanStatus.RETURNED
                ? Instant.parse("2026-08-10T10:00:00Z")
                : null;
        return new LoanResponse(
                id,
                7L,
                "Maria Silva",
                Instant.parse("2026-08-01T10:00:00Z"),
                LocalDate.parse("2026-08-15"),
                returnedAt,
                status
        );
    }
}
