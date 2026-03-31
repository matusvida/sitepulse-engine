package com.sitepulse.engine.plan.domain.port;

import java.util.List;

public interface PlanEvidenceImageProvider {

    List<byte[]> recentProjectImages(Integer projectId, int limit);
}
