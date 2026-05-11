package com.sitepulse.engine.auth.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProjectAccessRepository extends JpaRepository<UserProjectAccessEntity, UserProjectAccessId> {

    List<UserProjectAccessEntity> findByUserId(Integer userId);

    List<UserProjectAccessEntity> findByUserIdIn(Collection<Integer> userIds);

    boolean existsByUserIdAndProjectId(Integer userId, Integer projectId);

    void deleteByUserId(Integer userId);
}
