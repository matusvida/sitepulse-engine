package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.AuthUserResult;
import com.sitepulse.engine.auth.application.AuthUserResultFactory;
import com.sitepulse.engine.auth.application.AuthenticatedUser;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.port.UserAccountStore;
import com.sitepulse.engine.auth.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCurrentAuthSessionQuery {

    private final UserAccountStore userAccountStore;
    private final AuthUserResultFactory authUserResultFactory;

    @Transactional(readOnly = true)
    public AuthUserResult get(AuthenticatedUser authenticatedUser) {
        UserAccount user = userAccountStore.findById(authenticatedUser.id())
                .orElseThrow(() -> new UnauthorizedException("Session is no longer valid"));
        return authUserResultFactory.create(user);
    }
}
