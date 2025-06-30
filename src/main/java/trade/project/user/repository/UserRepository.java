package trade.project.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trade.project.user.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 사용자명으로 사용자 조회
    Optional<User> findByUsername(String username);

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 사용자명과 활성 상태로 사용자 조회
    Optional<User> findByUsernameAndIsActiveTrue(String username);

    // 이메일과 활성 상태로 사용자 조회
    Optional<User> findByEmailAndIsActiveTrue(String email);

    // 사용자명 존재 여부 확인
    boolean existsByUsername(String username);

    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);

    // 활성 상태인 사용자명 존재 여부 확인
    boolean existsByUsernameAndIsActiveTrue(String username);

    // 활성 상태인 이메일 존재 여부 확인
    boolean existsByEmailAndIsActiveTrue(String email);

    // 사용자명 또는 이메일로 사용자 조회 (로그인용)
    @Query("SELECT u FROM User u WHERE (u.username = :usernameOrEmail OR u.email = :usernameOrEmail) AND u.isActive = true")
    Optional<User> findByUsernameOrEmailAndIsActiveTrue(@Param("usernameOrEmail") String usernameOrEmail);
} 