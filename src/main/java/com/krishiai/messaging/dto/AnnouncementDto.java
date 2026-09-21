package com.krishiai.messaging.dto;

import com.krishiai.messaging.entity.Announcement;
import com.krishiai.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementDto {
    private Long id;
    private String title;
    private String content;
    private String createdByName;
    private UserRole targetRole;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static AnnouncementDto fromEntity(Announcement a) {
        return AnnouncementDto.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .createdByName(a.getCreatedBy() != null ? a.getCreatedBy().getFullName() : "KrishiAI")
                .targetRole(a.getTargetRole())
                .createdAt(a.getCreatedAt())
                .expiresAt(a.getExpiresAt())
                .build();
    }
}
