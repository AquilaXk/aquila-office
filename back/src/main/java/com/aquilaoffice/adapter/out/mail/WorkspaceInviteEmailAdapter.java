package com.aquilaoffice.adapter.out.mail;

import com.aquilaoffice.application.port.out.SendWorkspaceInviteEmailPort;
import com.aquilaoffice.application.port.out.WorkspaceInviteEmailCommand;
import com.aquilaoffice.global.config.properties.WorkspaceInviteEmailProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

/**
 * Spring MailSender로 워크스페이스 초대 메일을 발송하는 SMTP adapter다.
 */
@Component
@ConditionalOnProperty(prefix = "aquila.invite-email", name = "enabled", havingValue = "true")
public class WorkspaceInviteEmailAdapter implements SendWorkspaceInviteEmailPort {

    private static final DateTimeFormatter EXPIRES_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MailSender mailSender;
    private final WorkspaceInviteEmailProperties properties;

    public WorkspaceInviteEmailAdapter(MailSender mailSender, WorkspaceInviteEmailProperties properties) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public void send(WorkspaceInviteEmailCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (!properties.enabled()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(command.email());
        message.setSubject("[Aquila Office] " + command.workspaceName() + " 워크스페이스 초대");
        message.setText(body(command));
        mailSender.send(message);
    }

    private String body(WorkspaceInviteEmailCommand command) {
        return """
                Aquila Office 워크스페이스 초대가 도착했습니다.

                워크스페이스: %s
                초대 링크: %s
                만료 시각: %s
                """.formatted(
                command.workspaceName(),
                acceptUrl(command.token()),
                command.expiresAt().format(EXPIRES_FORMATTER));
    }

    private String acceptUrl(String token) {
        return properties.acceptUrlBase()
                + "?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
