package com.krishiai.messaging.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.messaging.dto.AnnouncementDto;
import com.krishiai.messaging.dto.CreateAnnouncementRequest;
import com.krishiai.messaging.service.AnnouncementService;
import com.krishiai.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Validated
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/announcements")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> getMyAnnouncements(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<AnnouncementDto> announcements =
                announcementService.getActiveAnnouncementsForUser(principal.getUser().getRole());
        return ResponseEntity.ok(ApiResponse.success("Announcements retrieved", announcements));
    }

    @PostMapping("/admin/announcements")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementDto>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        AnnouncementDto dto = announcementService.createAnnouncement(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Announcement created", dto));
    }
}
