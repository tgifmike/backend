package com.backend.backend.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    public EmailTemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String buildInviteEmail(
            String inviterName,
            String accountName,
            String appRole,
            String accessRole,
            String email,
            String loginUrl
    ) {

        Context context = new Context();
        context.setVariable("inviterName", inviterName);
        context.setVariable("accountName", accountName);
        context.setVariable("appRole", appRole);
        context.setVariable("accessRole", accessRole);
        context.setVariable("email", email);
        context.setVariable("loginUrl", loginUrl);

        return templateEngine.process("email/invite", context);
    }
}
