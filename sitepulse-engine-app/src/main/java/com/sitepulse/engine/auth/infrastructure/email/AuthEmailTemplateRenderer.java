package com.sitepulse.engine.auth.infrastructure.email;

import com.sitepulse.engine.auth.domain.model.AuthMailRecipient;
import com.sitepulse.engine.config.SitePulseProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
@Slf4j
public class AuthEmailTemplateRenderer {

    private static final String INVITATION_HTML_TEMPLATE = "classpath:templates/auth/invitation.html";
    private static final String INVITATION_TEXT_TEMPLATE = "classpath:templates/auth/invitation.txt";
    private static final String PASSWORD_RESET_HTML_TEMPLATE = "classpath:templates/auth/password-reset.html";
    private static final String PASSWORD_RESET_TEXT_TEMPLATE = "classpath:templates/auth/password-reset.txt";

    private final SitePulseProperties properties;
    private final String invitationHtmlTemplate;
    private final String invitationTextTemplate;
    private final String passwordResetHtmlTemplate;
    private final String passwordResetTextTemplate;

    public AuthEmailTemplateRenderer(ResourceLoader resourceLoader, SitePulseProperties properties) {
        this.properties = properties;
        this.invitationHtmlTemplate = loadTemplate(resourceLoader, INVITATION_HTML_TEMPLATE);
        this.invitationTextTemplate = loadTemplate(resourceLoader, INVITATION_TEXT_TEMPLATE);
        this.passwordResetHtmlTemplate = loadTemplate(resourceLoader, PASSWORD_RESET_HTML_TEMPLATE);
        this.passwordResetTextTemplate = loadTemplate(resourceLoader, PASSWORD_RESET_TEXT_TEMPLATE);
    }

    public AuthEmailContent renderInvitation(AuthMailRecipient user, String invitationUrl) {
        String appName = properties.auth().mail().appName();
        Map<String, String> model = baseModel(
                user,
                invitationUrl,
                "Set your password",
                "Activate your access to " + appName,
                properties.auth().invitationTtl().toHours()
        );
        return new AuthEmailContent(
                "You're invited to " + appName,
                renderHtml(invitationHtmlTemplate, model),
                renderText(invitationTextTemplate, model)
        );
    }

    public AuthEmailContent renderPasswordReset(AuthMailRecipient user, String resetUrl) {
        String appName = properties.auth().mail().appName();
        Map<String, String> model = baseModel(
                user,
                resetUrl,
                "Reset password",
                "Reset your " + appName + " password",
                properties.auth().passwordResetTtl().toHours()
        );
        return new AuthEmailContent(
                "Reset your " + appName + " password",
                renderHtml(passwordResetHtmlTemplate, model),
                renderText(passwordResetTextTemplate, model)
        );
    }

    private Map<String, String> baseModel(
            AuthMailRecipient user,
            String actionUrl,
            String actionLabel,
            String preheader,
            long expiryHours
    ) {
        Map<String, String> model = new LinkedHashMap<>();
        model.put("appName", properties.auth().mail().appName());
        model.put("greetingName", resolveGreetingName(user));
        model.put("recipientName", resolveDisplayName(user));
        model.put("email", user.email());
        model.put("actionUrl", actionUrl);
        model.put("actionLabel", actionLabel);
        model.put("expiryHours", Long.toString(expiryHours));
        model.put("preheader", preheader);
        model.put("currentYear", Integer.toString(OffsetDateTime.now().getYear()));
        return model;
    }

    private String renderHtml(String template, Map<String, String> model) {
        String rendered = template;
        for (Map.Entry<String, String> entry : model.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", HtmlUtils.htmlEscape(entry.getValue()));
        }
        return rendered;
    }

    private String renderText(String template, Map<String, String> model) {
        String rendered = template;
        for (Map.Entry<String, String> entry : model.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private String resolveGreetingName(AuthMailRecipient user) {
        if (user.firstName() != null && !user.firstName().isBlank()) {
            return user.firstName().trim();
        }
        return "there";
    }

    private String resolveDisplayName(AuthMailRecipient user) {
        String firstName = user.firstName() == null ? "" : user.firstName().trim();
        String lastName = user.lastName() == null ? "" : user.lastName().trim();
        String displayName = (firstName + " " + lastName).trim();
        return displayName.isBlank() ? user.email() : displayName;
    }

    private String loadTemplate(ResourceLoader resourceLoader, String location) {
        try (InputStream inputStream = resourceLoader.getResource(location).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load auth email template from " + location, ex);
        }
    }
}
