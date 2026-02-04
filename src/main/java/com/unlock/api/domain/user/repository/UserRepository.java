package com.unlock.api.domain.user.repository;

import com.unlock.api.domain.user.entity.AuthProvider;
import com.unlock.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findBySocialIdAndProvider(String socialId, AuthProvider provider);
    Optional<User> findByInviteCode(String inviteCode);
}
