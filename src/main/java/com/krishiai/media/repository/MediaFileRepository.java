package com.krishiai.media.repository;

import com.krishiai.media.entity.MediaFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    Optional<MediaFile> findByPublicId(String publicId);

    boolean existsByPublicId(String publicId);

    Page<MediaFile> findByUploadedById(Long userId, Pageable pageable);

    void deleteByPublicId(String publicId);
}
