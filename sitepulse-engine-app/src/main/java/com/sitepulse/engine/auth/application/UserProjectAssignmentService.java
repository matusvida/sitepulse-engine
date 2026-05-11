package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.infrastructure.persistence.UserProjectAccessEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.UserProjectAccessRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProjectAssignmentService {

    private final UserProjectAccessRepository userProjectAccessRepository;

    @Transactional(readOnly = true)
    public List<Integer> listProjectIds(Integer userId) {
        return userProjectAccessRepository.findByUserId(userId).stream()
                .map(UserProjectAccessEntity::getProjectId)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Integer, List<Integer>> groupProjectIds(List<Integer> userIds) {
        Map<Integer, List<Integer>> projectMap = new LinkedHashMap<>();
        userProjectAccessRepository.findByUserIdIn(userIds).forEach(access ->
                projectMap.computeIfAbsent(access.getUserId(), ignored -> new ArrayList<>()).add(access.getProjectId()));
        return projectMap;
    }

    @Transactional
    public void replaceProjectAssignments(Integer userId, Collection<Integer> projectIds) {
        userProjectAccessRepository.deleteByUserId(userId);
        OffsetDateTime now = OffsetDateTime.now();
        userProjectAccessRepository.saveAll(projectIds.stream()
                .distinct()
                .map(projectId -> UserProjectAccessEntity.builder()
                        .userId(userId)
                        .projectId(projectId)
                        .createdAt(now)
                        .build())
                .toList());
    }
}
