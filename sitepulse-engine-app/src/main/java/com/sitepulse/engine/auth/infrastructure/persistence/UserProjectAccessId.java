package com.sitepulse.engine.auth.infrastructure.persistence;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserProjectAccessId implements Serializable {
    private Integer userId;
    private Integer projectId;
}
