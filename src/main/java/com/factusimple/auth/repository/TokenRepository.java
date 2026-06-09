package com.factusimple.auth.repository;

import com.factusimple.auth.entity.Token;
import com.factusimple.auth.entity.TokenType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findByToken(String token);

    List<Token> findByUserIdAndRevokedFalse(UUID userId);

    @Modifying
    @Query("update Token t set t.revoked = true "
            + "where t.user.id = :userId and t.type = :type and t.revoked = false")
    void revokeAllByUserAndType(@Param("userId") UUID userId, @Param("type") TokenType type);
}
