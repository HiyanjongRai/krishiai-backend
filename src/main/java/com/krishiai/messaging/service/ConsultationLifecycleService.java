package com.krishiai.messaging.service;

import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.consultation.entity.Consultation;
import com.krishiai.consultation.entity.ConsultationPackage;
import com.krishiai.consultation.entity.ConsultationStatus;
import com.krishiai.consultation.repository.ConsultationPackageRepository;
import com.krishiai.consultation.repository.ConsultationRepository;
import com.krishiai.crop.entity.Crop;
import com.krishiai.crop.repository.CropRepository;
import com.krishiai.messaging.dto.ConsultationDetailDto;
import com.krishiai.messaging.dto.ConsultationRequestDto;
import com.krishiai.messaging.entity.*;
import com.krishiai.messaging.repository.*;
import com.krishiai.messaging.security.MessagingAuthorizationService;
import com.krishiai.messaging.websocket.WebSocketEventPublisher;
import com.krishiai.payment.entity.Payment;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationLifecycleService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationPackageRepository packageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;
    private final MessagingAuthorizationService authorizationService;
    private final PresenceService presenceService;
    private final WebSocketEventPublisher eventPublisher;

    @Transactional
    public ConsultationDetailDto requestConsultation(Long farmerId, ConsultationRequestDto request) {
        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));

        User expert = userRepository.findById(request.getExpertId())
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));

        if (expert.getRole() != UserRole.ROLE_EXPERT) {
            throw new BadRequestException("Selected user is not an expert");
        }

        Crop crop = null;
        if (request.getCropId() != null) {
            crop = cropRepository.findById(request.getCropId())
                    .orElseThrow(() -> new ResourceNotFoundException("Crop not found"));
        }

        ConsultationPackage packageEntity = null;
        if (request.getPackageId() != null) {
            packageEntity = packageRepository.findByIdWithDetails(request.getPackageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + request.getPackageId()));

            if (!packageEntity.getExpert().getId().equals(expert.getId())) {
                throw new BadRequestException("The chosen consultation package does not belong to this expert");
            }
            if (!Boolean.TRUE.equals(packageEntity.getActive())) {
                throw new BadRequestException("The chosen consultation package is currently inactive");
            }
            if (crop == null && packageEntity.getCrop() != null) {
                crop = packageEntity.getCrop();
            }
        } else {
            // Fallback to finding an active package for this expert & crop
            List<ConsultationPackage> activePackages = packageRepository.findActivePackagesForExpertWithCrop(expert.getId());
            if (!activePackages.isEmpty()) {
                packageEntity = activePackages.get(0);
                if (crop == null && packageEntity.getCrop() != null) {
                    crop = packageEntity.getCrop();
                }
            }
        }

        Consultation consultation = new Consultation(farmer, expert, crop, packageEntity, request.getSubject(), request.getDescription());
        consultation.setStatus(ConsultationStatus.REQUESTED);
        consultation = consultationRepository.save(consultation);

        log.info("Consultation requested: id={}, farmerId={}, expertId={}, price={}",
                consultation.getId(), farmerId, expert.getId(), consultation.getPriceAtPurchase());

        return ConsultationDetailDto.fromEntity(consultation, null,
                presenceService.isOnline(farmerId), presenceService.isOnline(expert.getId()));
    }

    @Transactional
    public ConsultationDetailDto acceptConsultation(Long expertId, Long consultationId) {
        Consultation consultation = consultationRepository.findByIdWithDetails(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));

        if (consultation.getExpert() == null || !consultation.getExpert().getId().equals(expertId)) {
            throw new ForbiddenException("You are not the assigned expert for this consultation");
        }

        if (consultation.getStatus() == ConsultationStatus.PAYMENT_PENDING) {
            log.info("Consultation already accepted and awaiting payment: id={}", consultationId);
            return ConsultationDetailDto.fromEntity(consultation, null,
                    presenceService.isOnline(consultation.getFarmer().getId()), presenceService.isOnline(expertId));
        }

        if (consultation.getStatus() == ConsultationStatus.ACTIVE) {
            log.info("Consultation already active: id={}", consultationId);
            Optional<Conversation> conv = conversationRepository.findByConsultationId(consultation.getId());
            return ConsultationDetailDto.fromEntity(consultation, conv.map(Conversation::getId).orElse(null),
                    presenceService.isOnline(consultation.getFarmer().getId()), presenceService.isOnline(expertId));
        }

        if (consultation.getStatus() != ConsultationStatus.REQUESTED && consultation.getStatus() != ConsultationStatus.PENDING) {
            throw new BadRequestException("Consultation is not in REQUESTED state. Current: " + consultation.getStatus());
        }

        boolean requiresPayment = consultation.getPriceAtPurchase() != null
                && consultation.getPriceAtPurchase().compareTo(java.math.BigDecimal.ZERO) > 0;

        if (!requiresPayment) {
            // Free consultation or no price set: activate immediately without requiring payment gateway
            consultation.setStatus(ConsultationStatus.ACTIVE);
            consultation.setAcceptedAt(LocalDateTime.now());
            consultation.setStartedAt(LocalDateTime.now());
            int durationHours = 168; // default 7 days
            if (consultation.getPackageEntity() != null && consultation.getPackageEntity().getDurationHours() != null) {
                durationHours = consultation.getPackageEntity().getDurationHours();
            }
            consultation.setExpiresAt(LocalDateTime.now().plusHours(durationHours));
            consultation = consultationRepository.save(consultation);

            Conversation conversation = createConsultationConversation(consultation);

            eventPublisher.broadcastConsultationUpdate(consultation.getId(), consultation.getStatus(),
                    conversation.getId(), List.of(consultation.getFarmer().getId(), expertId));

            log.info("Free consultation accepted and ACTIVATED immediately: id={}, expertId={}", consultationId, expertId);

            return ConsultationDetailDto.fromEntity(consultation, conversation.getId(),
                    presenceService.isOnline(consultation.getFarmer().getId()), presenceService.isOnline(expertId));
        }

        // Move to PAYMENT_PENDING. Do NOT activate chat or create conversation until payment is verified!
        consultation.setStatus(ConsultationStatus.PAYMENT_PENDING);
        consultation.setAcceptedAt(LocalDateTime.now());
        consultation = consultationRepository.save(consultation);

        // Broadcast consultation status update
        eventPublisher.broadcastConsultationUpdate(consultation.getId(), consultation.getStatus(),
                null, List.of(consultation.getFarmer().getId(), expertId));

        log.info("Consultation accepted by expert (awaiting payment): id={}, expertId={}", consultationId, expertId);

        return ConsultationDetailDto.fromEntity(consultation, null,
                presenceService.isOnline(consultation.getFarmer().getId()), presenceService.isOnline(expertId));
    }

    /**
     * Activates a consultation strictly after server-side payment verification has succeeded.
     * Sets startedAt, calculates expiresAt, and creates the real-time conversation.
     */
    @Transactional
    public void activatePaidConsultation(Payment payment) {
        Consultation consultation = payment.getConsultation();
        consultation = consultationRepository.findByIdWithDetails(consultation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found: " + payment.getConsultation().getId()));

        if (consultation.getStatus() == ConsultationStatus.ACTIVE) {
            log.info("Consultation #{} is already ACTIVE, returning existing conversation", consultation.getId());
            createConsultationConversation(consultation);
            return;
        }

        consultation.setStatus(ConsultationStatus.ACTIVE);
        consultation.setPaymentVerifiedAt(payment.getPaidAt() != null ? payment.getPaidAt() : LocalDateTime.now());
        consultation.setStartedAt(LocalDateTime.now());

        int durationHours = 168; // default 7 days
        if (consultation.getPackageEntity() != null && consultation.getPackageEntity().getDurationHours() != null) {
            durationHours = consultation.getPackageEntity().getDurationHours();
        }
        consultation.setExpiresAt(LocalDateTime.now().plusHours(durationHours));
        consultation = consultationRepository.save(consultation);

        // Activate conversation and participants
        Conversation conversation = createConsultationConversation(consultation);

        // Broadcast to farmer and expert
        eventPublisher.broadcastConsultationUpdate(consultation.getId(), consultation.getStatus(),
                conversation.getId(), List.of(consultation.getFarmer().getId(), consultation.getExpert().getId()));

        log.info("Consultation ACTIVATED after payment: id={}, conversationId={}, expiresAt={}",
                consultation.getId(), conversation.getId(), consultation.getExpiresAt());
    }

    @Transactional
    public ConsultationDetailDto rejectConsultation(Long expertId, Long consultationId) {
        Consultation consultation = consultationRepository.findByIdWithDetails(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));

        if (consultation.getExpert() == null || !consultation.getExpert().getId().equals(expertId)) {
            throw new ForbiddenException("You are not the assigned expert for this consultation");
        }

        if (consultation.getStatus() != ConsultationStatus.REQUESTED && consultation.getStatus() != ConsultationStatus.PENDING) {
            throw new BadRequestException("Consultation is not in REQUESTED state");
        }

        consultation.setStatus(ConsultationStatus.REJECTED);
        consultation = consultationRepository.save(consultation);

        log.info("Consultation rejected: id={}, expertId={}", consultationId, expertId);

        return ConsultationDetailDto.fromEntity(consultation, null,
                presenceService.isOnline(consultation.getFarmer().getId()), presenceService.isOnline(expertId));
    }

    @Transactional
    public ConsultationDetailDto completeConsultation(Long userId, Long consultationId, UserRole role) {
        Consultation consultation = consultationRepository.findByIdWithDetails(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));

        authorizationService.assertCanAccessConsultation(userId, consultationId, role);

        if (consultation.getStatus() != ConsultationStatus.ACTIVE && consultation.getStatus() != ConsultationStatus.ACCEPTED) {
            throw new BadRequestException("Consultation is not in an active state");
        }

        consultation.setStatus(ConsultationStatus.COMPLETED);
        consultation.setCompletedAt(LocalDateTime.now());
        final Consultation savedConsultation = consultationRepository.save(consultation);

        // Add system message to the conversation
        Optional<Conversation> conv = conversationRepository.findByConsultationId(consultationId);
        conv.ifPresent(c -> {
            Message closeMsg = new Message(c.getId(), savedConsultation.getFarmer(),
                    "This consultation was marked as completed. Thank you for using KrishiAI Expert Advisory.",
                    MessageType.SYSTEM);
            messageRepository.save(closeMsg);
            eventPublisher.broadcastMessage(closeMsg, c.getId(), false, "SENT");
        });

        log.info("Consultation completed: id={}, by userId={}", consultationId, userId);

        return ConsultationDetailDto.fromEntity(savedConsultation, conv.map(Conversation::getId).orElse(null),
                presenceService.isOnline(savedConsultation.getFarmer().getId()),
                savedConsultation.getExpert() != null ? presenceService.isOnline(savedConsultation.getExpert().getId()) : false);
    }

    @Transactional
    public ConsultationDetailDto cancelConsultation(Long farmerId, Long consultationId) {
        Consultation consultation = consultationRepository.findByIdWithDetails(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));

        if (!consultation.getFarmer().getId().equals(farmerId)) {
            throw new ForbiddenException("Only the farmer who requested this consultation can cancel it");
        }

        if (consultation.getStatus() != ConsultationStatus.REQUESTED &&
            consultation.getStatus() != ConsultationStatus.PENDING &&
            consultation.getStatus() != ConsultationStatus.PAYMENT_PENDING) {
            throw new BadRequestException("Cannot cancel a consultation that has already started or completed");
        }

        consultation.setStatus(ConsultationStatus.CANCELLED);
        consultation.setCancelledAt(LocalDateTime.now());
        consultation = consultationRepository.save(consultation);

        log.info("Consultation cancelled by farmer: id={}, farmerId={}", consultationId, farmerId);

        return ConsultationDetailDto.fromEntity(consultation, null,
                presenceService.isOnline(farmerId),
                consultation.getExpert() != null ? presenceService.isOnline(consultation.getExpert().getId()) : false);
    }

    @Transactional
    public List<ConsultationDetailDto> listConsultationsForUser(Long userId, UserRole role) {
        List<Consultation> consultations;
        if (role == UserRole.ROLE_ADMIN) {
            consultations = consultationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100)).getContent();
        } else if (role == UserRole.ROLE_EXPERT) {
            consultations = consultationRepository.findByExpertIdOrderByCreatedAtDesc(userId);
        } else {
            consultations = consultationRepository.findByFarmerIdOrderByCreatedAtDesc(userId);
        }

        LocalDateTime now = LocalDateTime.now();
        return consultations.stream().map(c -> {
            // Check for passive expiration
            if (c.getStatus() == ConsultationStatus.ACTIVE && c.isExpired()) {
                c.setStatus(ConsultationStatus.EXPIRED);
                c = consultationRepository.save(c);
            }

            Long convId = conversationRepository.findByConsultationId(c.getId())
                    .map(Conversation::getId).orElse(null);
            boolean farmerOnline = c.getFarmer() != null && presenceService.isOnline(c.getFarmer().getId());
            boolean expertOnline = c.getExpert() != null && presenceService.isOnline(c.getExpert().getId());
            return ConsultationDetailDto.fromEntity(c, convId, farmerOnline, expertOnline);
        }).collect(Collectors.toList());
    }

    @Transactional
    public ConsultationDetailDto getConsultationDetail(Long userId, Long consultationId, UserRole role) {
        Consultation c = authorizationService.assertCanAccessConsultation(userId, consultationId, role);

        // Check for passive expiration
        if (c.getStatus() == ConsultationStatus.ACTIVE && c.isExpired()) {
            c.setStatus(ConsultationStatus.EXPIRED);
            c = consultationRepository.save(c);
            log.info("Consultation id={} transitioned to EXPIRED upon retrieval", c.getId());
        }

        Long convId = conversationRepository.findByConsultationId(consultationId).map(Conversation::getId).orElse(null);
        boolean farmerOnline = c.getFarmer() != null && presenceService.isOnline(c.getFarmer().getId());
        boolean expertOnline = c.getExpert() != null && presenceService.isOnline(c.getExpert().getId());
        return ConsultationDetailDto.fromEntity(c, convId, farmerOnline, expertOnline);
    }

    /**
     * Scheduled background job running every minute to automatically transition expired consultations to EXPIRED.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireOutdatedConsultations() {
        List<Consultation> expiredList = consultationRepository.findActiveExpiredConsultations(LocalDateTime.now());
        for (Consultation c : expiredList) {
            c.setStatus(ConsultationStatus.EXPIRED);
            consultationRepository.save(c);

            Optional<Conversation> conv = conversationRepository.findByConsultationId(c.getId());
            conv.ifPresent(conversation -> {
                Message expireMsg = new Message(conversation.getId(), c.getFarmer(),
                        "The consultation duration of " +
                        (c.getPackageEntity() != null ? c.getPackageEntity().getDurationHours() / 24 + " days" : "7 days") +
                        " has concluded. Messaging is now closed.",
                        MessageType.SYSTEM);
                messageRepository.save(expireMsg);
            });

            log.info("Consultation #{} automatically EXPIRED by background scheduler", c.getId());
        }
    }

    private synchronized Conversation createConsultationConversation(Consultation consultation) {
        Optional<Conversation> existing = conversationRepository.findByConsultationId(consultation.getId());
        if (existing.isPresent()) return existing.get();

        try {
            String title = (consultation.getDisplaySubject() != null ? consultation.getDisplaySubject() : "Consultation")
                    + " - " + (consultation.getCrop() != null ? consultation.getCrop().getName() : "Agriculture");

            Conversation conversation = new Conversation(ConversationType.CONSULTATION, consultation.getId(), title);
            conversation = conversationRepository.saveAndFlush(conversation);

            // Add farmer and expert as members
            conversationMemberRepository.save(new ConversationMember(conversation.getId(), consultation.getFarmer()));
            if (consultation.getExpert() != null) {
                conversationMemberRepository.save(new ConversationMember(conversation.getId(), consultation.getExpert()));
            }

            // Add welcome system message
            String durationDays = consultation.getPackageEntity() != null
                    ? (consultation.getPackageEntity().getDurationHours() / 24) + " days"
                    : "7 days";

            Message welcomeMsg = new Message(conversation.getId(), consultation.getFarmer(),
                    "Payment verified. Live consultation started with " + consultation.getExpert().getFullName() +
                    ". Active for " + durationDays + ". You can now discuss your crop concerns and share field photos.",
                    MessageType.SYSTEM);
            messageRepository.save(welcomeMsg);

            return conversation;
        } catch (Exception ex) {
            log.info("Conversation already created or concurrent conflict for consultation #{}, fetching existing", consultation.getId());
            return conversationRepository.findByConsultationId(consultation.getId())
                    .orElseThrow(() -> new RuntimeException("Could not obtain conversation for consultation #" + consultation.getId(), ex));
        }
    }
}
