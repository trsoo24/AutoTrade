package trade.project.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trade.project.user.dto.LoginRequest;
import trade.project.user.dto.SignupRequest;
import trade.project.user.dto.UserResponse;
import trade.project.user.entity.User;
import trade.project.user.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     */
    @Transactional
    public UserResponse signup(SignupRequest request) {
        log.info("회원가입 요청: {}", request.getUsername());

        // 비밀번호 확인 검증
        if (!request.isPasswordMatching()) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 사용자명 중복 확인
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 사용자명입니다.");
        }

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // User 엔티티 생성
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .realName(request.getRealName())
                .phone(request.getPhone())
                .build();

        // 비밀번호 암호화
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // 저장
        User savedUser = userRepository.save(user);
        log.info("회원가입 완료: {}", savedUser.getUsername());

        return UserResponse.from(savedUser);
    }

    /**
     * 로그인
     */
    @Transactional
    public UserResponse login(LoginRequest request) {
        log.info("로그인 요청: {}", request.getUsername());

        // 사용자 조회 (사용자명 또는 이메일로)
        User user = userRepository.findByUsernameOrEmailAndIsActiveTrue(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자이거나 비활성화된 계정입니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 마지막 로그인 시간 업데이트
        user.updateLastLoginAt();
        userRepository.save(user);

        log.info("로그인 성공: {}", user.getUsername());
        return UserResponse.from(user);
    }

    /**
     * 사용자 정보 조회
     */
    public UserResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return UserResponse.from(user);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void withdraw(Long userId) {
        log.info("회원 탈퇴 요청: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("이미 탈퇴된 계정입니다.");
        }

        // 계정 비활성화
        user.deactivate();
        userRepository.save(user);

        log.info("회원 탈퇴 완료: {}", user.getUsername());
    }

    /**
     * 사용자명 중복 확인
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsernameAndIsActiveTrue(username);
    }

    /**
     * 이메일 중복 확인
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailAndIsActiveTrue(email);
    }
} 