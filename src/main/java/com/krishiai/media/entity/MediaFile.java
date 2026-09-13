package com.krishiai.media.entity;

import com.krishiai.common.audit.BaseEntity;
import com.krishiai.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(
        name = "media_files",
        indexes = {
                @Index(name = "idx_media_public_id", columnList = "public_id", unique = true),
                @Index(name = "idx_media_uploaded_by", columnList = "uploaded_by_id"),
                @Index(name = "idx_media_folder", columnList = "folder")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class MediaFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 255)
    private String publicId;

    @Column(name = "secure_url", nullable = false, columnDefinition = "TEXT")
    private String secureUrl;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "format", length = 30)
    private String format;

    @Column(name = "resource_type", nullable = false, length = 30)
    private String resourceType; // image, video, raw

    @Column(name = "bytes")
    private Long bytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "folder", length = 150)
    private String folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", foreignKey = @ForeignKey(name = "fk_media_uploaded_by"))
    private User uploadedBy;

    public MediaFile(String publicId, String secureUrl, String originalFilename,
                     String format, String resourceType, Long bytes,
                     Integer width, Integer height, String folder, User uploadedBy) {
        this.publicId = publicId;
        this.secureUrl = secureUrl;
        this.originalFilename = originalFilename;
        this.format = format;
        this.resourceType = resourceType;
        this.bytes = bytes;
        this.width = width;
        this.height = height;
        this.folder = folder;
        this.uploadedBy = uploadedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MediaFile that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
