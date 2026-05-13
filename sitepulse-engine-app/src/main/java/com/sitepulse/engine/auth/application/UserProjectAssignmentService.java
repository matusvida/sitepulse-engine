package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.port.UserProjectAccessStore;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProjectAssignmentService {

    private final UserProjectAccessStore userProjectAccessStore;

    @Transactional(readOnly = true)
    public List<Integer> listProjectIds(Integer userId) {
        return userProjectAccessStore.findProjectIdsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Map<Integer, List<Integer>> groupProjectIds(List<Integer> userIds) {
        return userProjectAccessStore.groupProjectIds(userIds);
    }

    @Transactional
    public void replaceProjectAssignments(Integer userId, Collection<Integer> projectIds) {
        userProjectAccessStore.replaceAssignments(userId, projectIds, OffsetDateTime.now());
    }
}
