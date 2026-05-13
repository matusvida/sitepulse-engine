package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.port.UserProjectAccessStore;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUserResultFactory {

    private final UserProjectAccessStore userProjectAccessStore;

    public AuthUserResult create(UserAccount user) {
        List<Integer> projectIds = userProjectAccessStore.findProjectIdsByUserId(user.id());
        return new AuthUserResult(
                user.id(),
                user.email(),
                user.firstName(),
                user.lastName(),
                user.role(),
                user.status(),
                projectIds
        );
    }
}
