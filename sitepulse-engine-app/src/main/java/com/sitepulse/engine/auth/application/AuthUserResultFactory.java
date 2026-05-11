package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.infrastructure.persistence.UserEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.UserProjectAccessEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.UserProjectAccessRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUserResultFactory {

    private final UserProjectAccessRepository userProjectAccessRepository;

    public AuthUserResult create(UserEntity user) {
        List<Integer> projectIds = userProjectAccessRepository.findByUserId(user.getId()).stream()
                .map(UserProjectAccessEntity::getProjectId)
                .toList();
        return new AuthUserResult(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getStatus(),
                projectIds
        );
    }
}
