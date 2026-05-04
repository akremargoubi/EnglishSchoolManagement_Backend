package com.esprit.notificationms.controller;

import com.esprit.notificationms.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmailController.class)
@DisplayName("EmailController — POST /api/emails/send")
class EmailControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean EmailService emailService;

    // ── valid request ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /send — valid body → 200 OK")
    void sendEmail_validBody_returns200() throws Exception {
        doNothing().when(emailService).sendEmail(any(), any());

        mockMvc.perform(post("/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Service-Origin", "auth-service")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "to",      "student@esm.com",
                                "subject", "Welcome",
                                "text",    "Hello!"
                        ))))
                .andExpect(status().isOk());

        verify(emailService).sendEmail(any(), eq("auth-service"));
    }

    @Test
    @DisplayName("POST /send — without X-Service-Origin header → 200 OK")
    void sendEmail_noOriginHeader_returns200() throws Exception {
        doNothing().when(emailService).sendEmail(any(), isNull());

        mockMvc.perform(post("/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "to",      "x@esm.com",
                                "subject", "Sub",
                                "text",    "Txt"
                        ))))
                .andExpect(status().isOk());
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /send — missing 'to' field → 400 Bad Request")
    void sendEmail_missingTo_returns400() throws Exception {
        mockMvc.perform(post("/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "subject", "Sub",
                                "text",    "Txt"
                        ))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("POST /send — invalid email format → 400 Bad Request")
    void sendEmail_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "to",      "not-an-email",
                                "subject", "Sub",
                                "text",    "Txt"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /send — empty subject → 400 Bad Request")
    void sendEmail_emptySubject_returns400() throws Exception {
        mockMvc.perform(post("/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "to",      "x@esm.com",
                                "subject", "",
                                "text",    "Txt"
                        ))))
                .andExpect(status().isBadRequest());
    }
}
