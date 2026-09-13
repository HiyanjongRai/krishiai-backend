package com.krishiai.consultation.service;

import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.consultation.dto.MessageResponse;
import com.krishiai.consultation.dto.SendMessageRequest;
import com.krishiai.consultation.entity.Consultation;
import com.krishiai.consultation.entity.ConsultationMessage;
import com.krishiai.consultation.repository.ConsultationMessageRepository;
import com.krishiai.consultation.repository.ConsultationRepository;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserStatus;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationMessageServiceImpl implements ConsultationMessageService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationMessageRepository messageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long consultationId, Long currentUserId, boolean isAdmin) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found with id: " + consultationId));

        validateParticipant(consultation, currentUserId, isAdmin);

        return messageRepository.findByConsultationIdOrderBySentAtAsc(consultationId)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long consultationId, Long currentUserId, SendMessageRequest request) {
        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        if (sender.getStatus() == UserStatus.BLOCKED || sender.getStatus() == UserStatus.SUSPENDED) {
            throw new ForbiddenException("Your account is " + sender.getStatus() + ". Messaging disabled.");
        }

        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found with id: " + consultationId));

        // Only the assigned farmer or assigned expert can post messages to the consultation
        boolean isFarmer = consultation.getFarmer().getId().equals(currentUserId);
        boolean isExpert = consultation.getExpert() != null && consultation.getExpert().getId().equals(currentUserId);

        if (!isFarmer && !isExpert) {
            throw new ForbiddenException("Access denied: You are not an active participant in consultation #" + consultationId);
        }

        ConsultationMessage message = new ConsultationMessage(
                consultation,
                sender,
                request.message().strip()
        );

        ConsultationMessage saved = messageRepository.save(message);
        log.info("New message sent in consultationId={} by userId={}", consultationId, currentUserId);
        return MessageResponse.from(saved);
    }

    private void validateParticipant(Consultation consultation, Long currentUserId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }

        boolean isFarmer = consultation.getFarmer().getId().equals(currentUserId);
        boolean isExpert = consultation.getExpert() != null && consultation.getExpert().getId().equals(currentUserId);

        if (!isFarmer && !isExpert) {
            throw new ForbiddenException("Access denied: You do not have permission to view consultation #" + consultation.getId());
        }
    }
}
