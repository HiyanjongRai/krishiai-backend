package com.krishiai.specialization.repository;

import com.krishiai.specialization.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, Long> {
    Optional<Specialization> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByName(String name);
    List<Specialization> findByActiveTrueOrderByNameAsc();
}
