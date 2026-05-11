package com.sitepulse.engine.auth.application;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class EmailAddressNormalizer {

    public String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
