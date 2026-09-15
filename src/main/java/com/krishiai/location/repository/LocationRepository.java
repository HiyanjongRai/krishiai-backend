package com.krishiai.location.repository;

import com.krishiai.location.entity.Location;
import com.krishiai.location.entity.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByTypeAndActiveTrueOrderByNameAsc(LocationType type);
    List<Location> findByTypeOrderByNameAsc(LocationType type);
    List<Location> findByParentIdAndActiveTrueOrderByNameAsc(Long parentId);
    List<Location> findByParentIdOrderByNameAsc(Long parentId);
    Optional<Location> findByNameAndType(String name, LocationType type);
    Optional<Location> findByNameIgnoreCaseAndType(String name, LocationType type);
    Optional<Location> findByNameIgnoreCaseAndParentIdAndType(String name, Long parentId, LocationType type);
    @Query("""
            SELECT l FROM Location l
            WHERE LOWER(l.name) = LOWER(:name)
              AND l.type = :type
              AND ((:parentId IS NULL AND l.parent IS NULL) OR l.parent.id = :parentId)
            """)
    Optional<Location> findByNameAndParentAndTypeIgnoreCase(
            @Param("name") String name,
            @Param("parentId") Long parentId,
            @Param("type") LocationType type
    );
    Optional<Location> findByCodeIgnoreCase(String code);
    boolean existsByNameAndType(String name, LocationType type);
    boolean existsByCodeIgnoreCase(String code);
}
