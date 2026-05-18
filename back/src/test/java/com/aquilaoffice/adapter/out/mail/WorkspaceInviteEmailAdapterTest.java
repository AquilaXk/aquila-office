package com.aquilaoffice.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.out.WorkspaceInviteEmailCommand;
import com.aquilaoffice.global.config.properties.WorkspaceInviteEmailProperties;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

class WorkspaceInviteEmailAdapterTest {

    @Test
    void sendsWorkspaceInviteEmailWhenEnabled() {
        RecordingMailSender mailSender = new RecordingMailSender();
        WorkspaceInviteEmailAdapter adapter = new WorkspaceInviteEmailAdapter(
                mailSender,
                new WorkspaceInviteEmailProperties(
                        true,
                        "noreply@aquila-office.local",
                        "https://office.example.com/invites/accept"));

        adapter.send(command());

        assertThat(mailSender.messages).singleElement().satisfies(message -> {
            assertThat(message.getFrom()).isEqualTo("noreply@aquila-office.local");
            assertThat(message.getTo()).containsExactly("invitee@example.com");
            assertThat(message.getSubject()).isEqualTo("[Aquila Office] Team 워크스페이스 초대");
            assertThat(message.getText())
                    .contains("Team")
                    .contains("https://office.example.com/invites/accept?token=generated-token")
                    .contains("2026-05-22T10:00");
        });
    }

    @Test
    void skipsSendWhenInviteEmailIsDisabled() {
        RecordingMailSender mailSender = new RecordingMailSender();
        WorkspaceInviteEmailAdapter adapter = new WorkspaceInviteEmailAdapter(
                mailSender,
                new WorkspaceInviteEmailProperties(
                        false,
                        "noreply@aquila-office.local",
                        "https://office.example.com/invites/accept"));

        adapter.send(command());

        assertThat(mailSender.messages).isEmpty();
    }

    private static WorkspaceInviteEmailCommand command() {
        return new WorkspaceInviteEmailCommand(
                501L,
                101L,
                "Team",
                "invitee@example.com",
                "generated-token",
                LocalDateTime.of(2026, 5, 22, 10, 0));
    }

    private static class RecordingMailSender implements MailSender {
        private final List<SimpleMailMessage> messages = new ArrayList<>();

        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            messages.add(simpleMessage);
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            messages.addAll(List.of(simpleMessages));
        }
    }
}
