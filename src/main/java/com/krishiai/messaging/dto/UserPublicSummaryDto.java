package com.krishiai.messaging.dto;

import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPublicSummaryDto {
    private Long id;
    private String displayName;
    private String profileImageUrl;
    private UserRole role;
    private Boolean online;

    public static UserPublicSummaryDto fromUser(User user, boolean online) {
        if (user == null) return null;
        String name = user.getFullName();
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            name = "KrishiAI Support";
        }
        return UserPublicSummaryDto.builder()
                .id(user.getId())
                .displayName(name)
                .profileImageUrl(user.getProfileImage())
                .role(user.getRole())
                .online(online)
                .build();
    }
}
