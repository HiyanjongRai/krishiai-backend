package com.krishiai.consultation.service;

import com.krishiai.consultation.dto.MessageResponse;
import com.krishiai.consultation.dto.SendMessageRequest;

import java.util.List;

public interface ConsultationMessageService {

    List<MessageResponse> getMessages(Long consultationId, Long currentUserId, boolean isAdmin);

    MessageResponse sendMessage(Long consultationId, Long currentUserId, SendMessageRequest request);
}
