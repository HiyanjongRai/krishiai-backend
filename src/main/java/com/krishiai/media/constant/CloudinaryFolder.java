package com.krishiai.media.constant;

import lombok.Getter;

@Getter
public enum CloudinaryFolder {
    USER_PROFILES("krishiai/users/profile"),
    EXPERT_DOCUMENTS("krishiai/experts/documents"),
    FARMER_IMAGES("krishiai/farmers/images"),
    CROP_IMAGES("krishiai/crops"),
    DISEASE_IMAGES("krishiai/diseases"),
    AI_ANALYSIS("krishiai/ai-analysis"),
    GENERAL("krishiai/media");

    private final String path;

    CloudinaryFolder(String path) {
        this.path = path;
    }

    public static CloudinaryFolder fromString(String value) {
        if (value == null || value.isBlank()) {
            return GENERAL;
        }
        for (CloudinaryFolder folder : values()) {
            if (folder.name().equalsIgnoreCase(value) || folder.getPath().equalsIgnoreCase(value)) {
                return folder;
            }
        }
        return GENERAL;
    }
}
