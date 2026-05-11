package com.sitepulse.engine.http.auth.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AuthUserView {

    private Integer id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String status;
    private List<Integer> projectIds;
}
