package trade.project.user.dto;

import lombok.*;
import trade.project.user.entity.User;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserResponse {

    private Long userId;
    private String username;
    private String email;
    private String realName;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
    private LocalDateTime lastLoginAt;

    // User 엔티티를 UserResponse로 변환
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
} 