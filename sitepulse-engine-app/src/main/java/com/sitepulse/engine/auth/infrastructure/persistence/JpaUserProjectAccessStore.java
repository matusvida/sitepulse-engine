package com.sitepulse.engine.auth.infrastructure.persistence;

import com.sitepulse.engine.auth.domain.port.UserProjectAccessStore;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaUserProjectAccessStore implements UserProjectAccessStore {

    private final UserProjectAccessRepository userProjectAccessRepository;

    @Override
    public List<Integer> findProjectIdsByUserId(Integer userId) {
        return userProjectAccessRepository.findByUserId(userId).stream()
                .map(UserProjectAccessEntity::getProjectId)
                .toList();
    }

    @Override
    public Map<Integer, List<Integer>> groupProjectIds(Collection<Integer> userIds) {
        Map<Integer, List<Integer>> projectMap = new LinkedHashMap<>();
        userProjectAccessRepository.findByUserIdIn(userIds).forEach(access ->
                projectMap.computeIfAbsent(access.getUserId(), ignored -> new ArrayList<>()).add(access.getProjectId()));
        return projectMap;
    }

    @Override
    public Set<Integer> findProjectIdSetByUserId(Integer userId) {
        return userProjectAccessRepository.findByUserId(userId).stream()
                .map(UserProjectAccessEntity::getProjectId)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean existsByUserIdAndProjectId(Integer userId, Integer projectId) {
        return userProjectAccessRepository.existsByUserIdAndProjectId(userId, projectId);
    }

    @Override
    public void replaceAssignments(Integer userId, Collection<Integer> projectIds, OffsetDateTime createdAt) {
        userProjectAccessRepository.deleteByUserId(userId);
        userProjectAccessRepository.saveAll(projectIds.stream()
                .distinct()
                .map(projectId -> UserProjectAccessEntity.builder()
                        .userId(userId)
                        .projectId(projectId)
                        .createdAt(createdAt)
                        .build())
                .toList());
    }
}
