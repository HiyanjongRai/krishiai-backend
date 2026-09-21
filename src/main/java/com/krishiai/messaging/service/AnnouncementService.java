package com.krishiai.messaging.service;

import com.krishiai.messaging.dto.AnnouncementDto;
import com.krishiai.messaging.dto.CreateAnnouncementRequest;
import com.krishiai.messaging.entity.Announcement;
import com.krishiai.messaging.repository.AnnouncementRepository;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.repository.UserRepository;
import com.krishiai.common.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public AnnouncementDto createAnnouncement(Long adminId, CreateAnnouncementRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        Announcement announcement = new Announcement(
                request.getTitle(),
                request.getContent(),
                admin,
                request.getTargetRole(),
                request.getExpiresAt()
        );

        announcement = announcementRepository.save(announcement);

        AnnouncementDto dto = AnnouncementDto.fromEntity(announcement);

        // Broadcast to appropriate topic via WebSocket
        String topic = request.getTargetRole() != null
                ? "/topic/announcements." + request.getTargetRole().name().toLowerCase()
                : "/topic/announcements.all";
        messagingTemplate.convertAndSend(topic, dto);

        log.info("Announcement created: id={}, adminId={}, targetRole={}", announcement.getId(), adminId, request.getTargetRole());
        return dto;
    }

    public List<AnnouncementDto> getActiveAnnouncementsForUser(UserRole role) {
        return announcementRepository.findActiveAnnouncementsForRole(role, LocalDateTime.now())
                .stream()
                .map(AnnouncementDto::fromEntity)
                .collect(Collectors.toList());
    }
}
