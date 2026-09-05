package com.krishiai.location.repository;

import com.krishiai.location.entity.Location;
import com.krishiai.location.entity.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByTypeAndActiveTrueOrderByNameAsc(LocationType type);
    List<Location> findByParentIdAndActiveTrueOrderByNameAsc(Long parentId);
    Optional<Location> findByNameAndType(String name, LocationType type);
    boolean existsByNameAndType(String name, LocationType type);
}
