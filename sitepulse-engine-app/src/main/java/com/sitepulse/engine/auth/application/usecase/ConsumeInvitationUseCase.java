package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.InvitationFlowService;
import com.sitepulse.engine.auth.application.SessionLifecycleService;
import com.sitepulse.engine.auth.application.SessionLoginResult;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsumeInvitationUseCase {

    private final InvitationFlowService invitationFlowService;
    private final SessionLifecycleService sessionLifecycleService;

    @Transactional
    public SessionLoginResult consume(String token, String firstName, String lastName, String password) {
        UserAccount user = invitationFlowService.consumeInvitation(token, firstName, lastName, password);
        return sessionLifecycleService.createSession(user);
    }
}
