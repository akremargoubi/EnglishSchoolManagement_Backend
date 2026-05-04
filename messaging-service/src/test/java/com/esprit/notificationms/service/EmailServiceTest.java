package com.esprit.notificationms.service;

import com.esprit.notificationms.dto.EmailRequest;
import com.esprit.notificationms.entity.EmailLog;
import com.esprit.notificationms.enums.EmailStatus;
import com.esprit.notificationms.repository.EmailLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService — send email and log behaviour")
class EmailServiceTest {

    @Mock EmailLogRepository emailLogRepository;

    @InjectMocks EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "apiKey",     "test-key");
        ReflectionTestUtils.setField(emailService, "secretKey",  "test-secret");
        ReflectionTestUtils.setField(emailService, "senderEmail","noreply@test.com");
        ReflectionTestUtils.setField(emailService, "senderName", "Test ESM");

        // Inject a spy RestTemplate so we can stub postForEntity
        RestTemplate spyTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(emailService, "restTemplate", spyTemplate);
    }

    private RestTemplate getRestTemplate() {
        return (RestTemplate) ReflectionTestUtils.getField(emailService, "restTemplate");
    }

    // ── successful send ───────────────────────────────────────────────────────

    @Test
    @DisplayName("sendEmail — success → log saved with status SENT")
    void sendEmail_success_logStatusIsSent() {
        when(getRestTemplate().postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(null);
        when(emailLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmailRequest req = buildRequest("student@esm.com", "Hello", "Welcome!");
        emailService.sendEmail(req, "auth-service");

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository).save(captor.capture());

        EmailLog log = captor.getValue();
        assertThat(log.getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(log.getRecipient()).isEqualTo("student@esm.com");
        assertThat(log.getSubject()).isEqualTo("Hello");
        assertThat(log.getServiceOrigin()).isEqualTo("auth-service");
        assertThat(log.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("sendEmail — null serviceOrigin → log saved without origin")
    void sendEmail_nullOrigin_logSavedWithoutOrigin() {
        when(getRestTemplate().postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(null);
        when(emailLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        emailService.sendEmail(buildRequest("x@esm.com", "Sub", "Txt"));

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository).save(captor.capture());
        assertThat(captor.getValue().getServiceOrigin()).isNull();
    }

    // ── failed send ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendEmail — Mailjet unreachable → log saved with status FAILED")
    void sendEmail_mailjetFails_logStatusIsFailed() {
        when(getRestTemplate().postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));
        when(emailLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        emailService.sendEmail(buildRequest("fail@esm.com", "Fail", "Body"), null);

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository).save(captor.capture());

        EmailLog log = captor.getValue();
        assertThat(log.getStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(log.getErrorMessage()).contains("Connection refused");
    }

    @Test
    @DisplayName("sendEmail — failure still saves the log (no exception propagated)")
    void sendEmail_failure_doesNotPropagateException() {
        when(getRestTemplate().postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RestClientException("Timeout"));
        when(emailLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> emailService.sendEmail(buildRequest("x@test.com", "S", "T"), null))
                .doesNotThrowAnyException();
    }

    // ── log always saved ──────────────────────────────────────────────────────

    @Test
    @DisplayName("sendEmail — log is always persisted regardless of outcome")
    void sendEmail_alwaysPersistsLog() {
        when(getRestTemplate().postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(null);
        when(emailLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        emailService.sendEmail(buildRequest("a@b.com", "S", "T"));

        verify(emailLogRepository, times(1)).save(any(EmailLog.class));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private EmailRequest buildRequest(String to, String subject, String text) {
        EmailRequest r = new EmailRequest();
        r.setTo(to);
        r.setSubject(subject);
        r.setText(text);
        return r;
    }
}
