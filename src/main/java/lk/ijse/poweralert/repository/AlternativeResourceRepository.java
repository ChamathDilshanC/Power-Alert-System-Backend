package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.AlternativeResource;
import lk.ijse.poweralert.enums.AppEnums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlternativeResourceRepository extends JpaRepository<AlternativeResource, Long> {

    /**
     * Find all active alternative resources
     * @return list of active resources
     */
    List<AlternativeResource> findByIsActiveTrue();

    /**
     * Find active alternative resources by area ID
     * @param areaId the area ID
     * @return list of active resources in the area
     */
    List<AlternativeResource> findByAreaIdAndIsActiveTrue(Long areaId);

    /**
     * Find active alternative resources by type
     * @param type the resource type
     * @return list of active resources of the specified type
     */
    List<AlternativeResource> findByTypeAndIsActiveTrue(ResourceType type);

    /**
     * Find active alternative resources by type and area ID
     * @param type the resource type
     * @param areaId the area ID
     * @return list of active resources of the specified type in the area
     */
    List<AlternativeResource> findByTypeAndAreaIdAndIsActiveTrue(ResourceType type, Long areaId);

    /**
     * Find active alternative resources within a certain distance from coordinates
     * Using native query for geospatial calculations
     */
    // This would typically involve a native query with the haversine formula
    // or a spatial function if your database supports it
}