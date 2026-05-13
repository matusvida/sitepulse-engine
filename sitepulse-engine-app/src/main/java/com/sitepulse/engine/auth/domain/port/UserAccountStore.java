package com.sitepulse.engine.auth.domain.port;

import com.sitepulse.engine.auth.domain.model.EmailAddress;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import java.util.List;
import java.util.Optional;

public interface UserAccountStore {

    Optional<UserAccount> findById(Integer userId);

    Optional<UserAccount> findByEmail(EmailAddress emailAddress);

    List<UserAccount> findAll();

    UserAccount save(UserAccount userAccount);
}
