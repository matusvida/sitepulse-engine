package com.sitepulse.engine.auth.domain.port;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserProjectAccessStore {

    List<Integer> findProjectIdsByUserId(Integer userId);

    Map<Integer, List<Integer>> groupProjectIds(Collection<Integer> userIds);

    Set<Integer> findProjectIdSetByUserId(Integer userId);

    boolean existsByUserIdAndProjectId(Integer userId, Integer projectId);

    void replaceAssignments(Integer userId, Collection<Integer> projectIds, OffsetDateTime createdAt);
}
