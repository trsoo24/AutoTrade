package trade.project.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trade.project.user.dto.LoginRequest;
import trade.project.user.dto.SignupRequest;
import trade.project.user.entity.User;
import trade.project.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 서비스 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        signupRequest = SignupRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("TestPass123!")
                .confirmPassword("TestPass123!")
                .realName("테스트 사용자")
                .phone("010-1234-5678")
                .build();

        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("TestPass123!")
                .build();

        testUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .realName("테스트 사용자")
                .isActive(true)
                .build();
        testUser.setPasswordHash("$2a$10$dummy.hash.for.testing");
    }

    @Test
    @DisplayName("회원가입 - 성공")
    void signup_Success() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        var result = userService.signup(signupRequest);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 - 비밀번호 불일치")
    void signup_PasswordMismatch() {
        // Given
        signupRequest.setConfirmPassword("WrongPassword123!");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signup(signupRequest);
        });
    }

    @Test
    @DisplayName("회원가입 - 사용자명 중복")
    void signup_UsernameExists() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signup(signupRequest);
        });
    }

    @Test
    @DisplayName("로그인 - 성공")
    void login_Success() {
        // Given
        when(userRepository.findByUsernameOrEmailAndIsActiveTrue("testuser"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        var result = userService.login(loginRequest);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 - 사용자 없음")
    void login_UserNotFound() {
        // Given
        when(userRepository.findByUsernameOrEmailAndIsActiveTrue("testuser"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login(loginRequest);
        });
    }

    @Test
    @DisplayName("로그인 - 비밀번호 불일치")
    void login_WrongPassword() {
        // Given
        when(userRepository.findByUsernameOrEmailAndIsActiveTrue("testuser"))
                .thenReturn(Optional.of(testUser));

        loginRequest.setPassword("WrongPassword123!");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login(loginRequest);
        });
    }

    @Test
    @DisplayName("회원 탈퇴 - 성공")
    void withdraw_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.withdraw(1L);

        // Then
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원 탈퇴 - 사용자 없음")
    void withdraw_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.withdraw(1L);
        });
    }

    @Test
    @DisplayName("사용자명 중복 확인 - 사용 가능")
    void isUsernameAvailable_Available() {
        // Given
        when(userRepository.existsByUsernameAndIsActiveTrue("testuser")).thenReturn(false);

        // When
        boolean result = userService.isUsernameAvailable("testuser");

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("사용자명 중복 확인 - 사용 불가")
    void isUsernameAvailable_NotAvailable() {
        // Given
        when(userRepository.existsByUsernameAndIsActiveTrue("testuser")).thenReturn(true);

        // When
        boolean result = userService.isUsernameAvailable("testuser");

        // Then
        assertFalse(result);
    }
} 