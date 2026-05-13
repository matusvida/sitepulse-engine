package com.sitepulse.engine.auth.infrastructure.persistence;

import com.sitepulse.engine.auth.domain.model.EmailAddress;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.port.UserAccountStore;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaUserAccountStore implements UserAccountStore {

    private final UserRepository userRepository;
    private final AuthPersistenceMapper authPersistenceMapper;

    @Override
    public Optional<UserAccount> findById(Integer userId) {
        return userRepository.findById(userId).map(authPersistenceMapper::toDomain);
    }

    @Override
    public Optional<UserAccount> findByEmail(EmailAddress emailAddress) {
        return userRepository.findByEmailIgnoreCase(emailAddress.value()).map(authPersistenceMapper::toDomain);
    }

    @Override
    public List<UserAccount> findAll() {
        return userRepository.findAll().stream().map(authPersistenceMapper::toDomain).toList();
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        return authPersistenceMapper.toDomain(userRepository.save(authPersistenceMapper.toEntity(userAccount)));
    }
}
