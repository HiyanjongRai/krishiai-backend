package com.krishiai.crop.repository;

import com.krishiai.crop.entity.Crop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CropRepository extends JpaRepository<Crop, Long> {
    Optional<Crop> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<Crop> findByActiveTrueOrderByNameAsc();
    Page<Crop> findAllByActiveTrueOrderByNameAsc(Pageable pageable);
    Page<Crop> findByCategoryIdAndActiveTrueOrderByNameAsc(Long categoryId, Pageable pageable);

    @Query("SELECT c FROM Crop c JOIN FETCH c.category WHERE c.active = true " +
           "AND (:categoryId IS NULL OR c.category.id = :categoryId) " +
           "AND (LOWER(c.name) LIKE CONCAT('%', :search, '%') " +
           "OR LOWER(c.nepaliName) LIKE CONCAT('%', :search, '%')) " +
           "ORDER BY c.name ASC")
    Page<Crop> searchActiveCrops(
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            Pageable pageable
    );
}
