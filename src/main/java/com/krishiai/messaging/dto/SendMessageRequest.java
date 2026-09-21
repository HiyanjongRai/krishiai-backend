package com.krishiai.messaging.dto;

import com.krishiai.messaging.entity.MessageType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @Size(max = 5000, message = "Message content cannot exceed 5000 characters")
    private String content;

    private MessageType messageType = MessageType.TEXT;

    @Size(max = 500, message = "Attachment URL too long")
    private String attachmentUrl;

    @Size(max = 255, message = "Attachment public ID too long")
    private String attachmentPublicId;

    @Size(max = 100, message = "MIME type too long")
    private String attachmentMimeType;

    private Long attachmentFileSize;

    private String clientMessageId;
}
