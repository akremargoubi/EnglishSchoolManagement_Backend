package com.esprit.notificationms.service;

import com.esprit.notificationms.entity.ScheduledEmail;
import com.esprit.notificationms.enums.EmailStatus;
import com.esprit.notificationms.repository.EmailLogRepository;
import com.esprit.notificationms.repository.ScheduledEmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledEmailService — scheduling and processing")
class ScheduledEmailServiceTest {

    @Mock ScheduledEmailRepository scheduledEmailRepository;
    @Mock EmailLogRepository emailLogRepository;

    @InjectMocks ScheduledEmailService scheduledEmailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduledEmailService, "apiKey",    "test-key");
        ReflectionTestUtils.setField(scheduledEmailService, "secretKey", "test-secret");
        ReflectionTestUtils.setField(scheduledEmailService, "senderEmail", "noreply@test.com");
        ReflectionTestUtils.setField(scheduledEmailService, "senderName",  "Test");
    }

    // ── scheduleEmail ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("scheduleEmail — status set to PENDING, createdAt populated")
    void scheduleEmail_setsPendingStatus() {
        ScheduledEmail input = new ScheduledEmail();
        input.setRecipient("student@esm.com");
        input.setSubject("Test");
        input.setText("Body");
        input.setScheduledAt(LocalDateTime.now().plusHours(1));

        when(scheduledEmailRepository.save(any())).thenAnswer(inv -> {
            ScheduledEmail e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        ScheduledEmail saved = scheduledEmailService.scheduleEmail(input);

        assertThat(saved.getStatus()).isEqualTo(EmailStatus.PENDING);
        assertThat(saved.getCreatedAt()).isNotNull();
        verify(scheduledEmailRepository).save(input);
    }

    // ── cancelScheduledEmail ──────────────────────────────────────────────────

    @Test
    @DisplayName("cancelScheduledEmail — PENDING email is cancelled")
    void cancelScheduledEmail_pending_setCancelled() {
        ScheduledEmail pending = new ScheduledEmail();
        pending.setId(1L);
        pending.setStatus(EmailStatus.PENDING);

        when(scheduledEmailRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(scheduledEmailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScheduledEmail result = scheduledEmailService.cancelScheduledEmail(1L);

        assertThat(result.getStatus()).isEqualTo(EmailStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelScheduledEmail — SENT email throws exception")
    void cancelScheduledEmail_alreadySent_throwsException() {
        ScheduledEmail sent = new ScheduledEmail();
        sent.setId(2L);
        sent.setStatus(EmailStatus.SENT);

        when(scheduledEmailRepository.findById(2L)).thenReturn(Optional.of(sent));

        assertThatThrownBy(() -> scheduledEmailService.cancelScheduledEmail(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("cancelScheduledEmail — unknown id throws exception")
    void cancelScheduledEmail_notFound_throwsException() {
        when(scheduledEmailRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduledEmailService.cancelScheduledEmail(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ── getAll / findByStatus / getById ───────────────────────────────────────

    @Test
    @DisplayName("getAll — returns all scheduled emails")
    void getAll_returnsAllEmails() {
        ScheduledEmail e1 = new ScheduledEmail(); e1.setId(1L);
        ScheduledEmail e2 = new ScheduledEmail(); e2.setId(2L);

        when(scheduledEmailRepository.findAll()).thenReturn(List.of(e1, e2));

        assertThat(scheduledEmailService.getAll()).hasSize(2);
    }

    @Test
    @DisplayName("findByStatus — returns only emails with given status")
    void findByStatus_returnsPending() {
        ScheduledEmail e = new ScheduledEmail();
        e.setStatus(EmailStatus.PENDING);

        when(scheduledEmailRepository.findByStatus(EmailStatus.PENDING)).thenReturn(List.of(e));

        List<ScheduledEmail> result = scheduledEmailService.findByStatus(EmailStatus.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(EmailStatus.PENDING);
    }

    @Test
    @DisplayName("getById — existing id returns email")
    void getById_existingId_returnsEmail() {
        ScheduledEmail e = new ScheduledEmail(); e.setId(5L);
        when(scheduledEmailRepository.findById(5L)).thenReturn(Optional.of(e));

        assertThat(scheduledEmailService.getById(5L).getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getById — unknown id throws exception")
    void getById_unknownId_throwsException() {
        when(scheduledEmailRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduledEmailService.getById(99L))
                .isInstanceOf(RuntimeException.class);
    }

    // ── processScheduledEmails ────────────────────────────────────────────────

    @Test
    @DisplayName("processScheduledEmails — no due emails → nothing sent")
    void processScheduledEmails_noDueEmails_doesNothing() {
        when(scheduledEmailRepository.findByStatusAndScheduledAtBefore(
                eq(EmailStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduledEmailService.processScheduledEmails();

        verify(emailLogRepository, never()).save(any());
    }
}
