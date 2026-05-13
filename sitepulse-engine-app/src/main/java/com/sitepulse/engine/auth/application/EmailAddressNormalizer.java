package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.model.EmailAddress;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class EmailAddressNormalizer {

    public EmailAddress normalize(String email) {
        return new EmailAddress(email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }
}
