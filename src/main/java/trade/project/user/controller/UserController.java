package trade.project.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trade.project.common.dto.ApiResponse;
import trade.project.user.dto.LoginRequest;
import trade.project.user.dto.SignupRequest;
import trade.project.user.dto.UserResponse;
import trade.project.user.service.UserService;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest request) {
        try {
            UserResponse userResponse = userService.signup(request);
            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (IllegalArgumentException e) {
            log.warn("회원가입 실패: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("SIGNUP_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("INTERNAL_ERROR", "회원가입 중 오류가 발생했습니다."));
        }
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            UserResponse userResponse = userService.login(request);
            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (IllegalArgumentException e) {
            log.warn("로그인 실패: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("LOGIN_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("로그인 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("INTERNAL_ERROR", "로그인 중 오류가 발생했습니다."));
        }
    }

    /**
     * 사용자 정보 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserInfo(@PathVariable Long userId) {
        try {
            UserResponse userResponse = userService.getUserInfo(userId);
            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (IllegalArgumentException e) {
            log.warn("사용자 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("USER_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("INTERNAL_ERROR", "사용자 정보 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> withdraw(@PathVariable Long userId) {
        try {
            userService.withdraw(userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            log.warn("회원 탈퇴 실패: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("WITHDRAW_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("회원 탈퇴 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("INTERNAL_ERROR", "회원 탈퇴 중 오류가 발생했습니다."));
        }
    }

    /**
     * 사용자명 중복 확인
     */
    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(@RequestParam String username) {
        try {
            boolean isAvailable = userService.isUsernameAvailable(username);
            return ResponseEntity.ok(ApiResponse.success(isAvailable));
        } catch (Exception e) {
            log.error("사용자명 중복 확인 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("INTERNAL_ERROR", "사용자명 중복 확인 중 오류가 발생했습니다."));
        }
    }

    /**
     * 이메일 중복 확인
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        try {
            boolean isAvailable = userService.isEmailAvailable(email);
            return ResponseEntity.ok(ApiResponse.success(isAvailable));
        } catch (Exception e) {
            log.error("이메일 중복 확인 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("INTERNAL_ERROR", "이메일 중복 확인 중 오류가 발생했습니다."));
        }
    }
} 