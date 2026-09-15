package com.krishiai.crop.repository;

import com.krishiai.crop.entity.CropCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CropCategoryRepository extends JpaRepository<CropCategory, Long> {
    Optional<CropCategory> findByCode(String code);
    Optional<CropCategory> findByCodeIgnoreCase(String code);
    Optional<CropCategory> findByNameIgnoreCase(String name);
    boolean existsByCode(String code);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByName(String name);
    boolean existsByNameIgnoreCase(String name);
    List<CropCategory> findByActiveTrueOrderByNameAsc();
}
