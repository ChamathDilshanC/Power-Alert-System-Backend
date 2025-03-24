package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.OutageGeospatialData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutageGeospatialRepository extends JpaRepository<OutageGeospatialData, Long> {

    /**
     * Find geospatial data by outage ID
     * @param outageId the outage ID
     * @return optional geospatial data
     */
    Optional<OutageGeospatialData> findByOutageId(Long outageId);

    /**
     * Find outages that contain a specific point (latitude, longitude)
     * This is a basic implementation that relies on the bounding box
     * For more accurate results, use proper geospatial queries with PostGIS
     *
     * @param latitude the latitude
     * @param longitude the longitude
     * @return list of geospatial data objects
     */
    @Query("SELECT g FROM OutageGeospatialData g WHERE " +
            "g.boundingBoxNorth >= :latitude AND g.boundingBoxSouth <= :latitude AND " +
            "g.boundingBoxEast >= :longitude AND g.boundingBoxWest <= :longitude")
    List<OutageGeospatialData> findOutagesContainingPoint(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude);

    /**
     * Find outages within a bounding box
     *
     * @param north the northern boundary
     * @param south the southern boundary
     * @param east the eastern boundary
     * @param west the western boundary
     * @return list of geospatial data objects
     */
    @Query("SELECT g FROM OutageGeospatialData g WHERE " +
            "(g.boundingBoxNorth <= :north AND g.boundingBoxNorth >= :south OR " +
            "g.boundingBoxSouth <= :north AND g.boundingBoxSouth >= :south) AND " +
            "(g.boundingBoxEast <= :east AND g.boundingBoxEast >= :west OR " +
            "g.boundingBoxWest <= :east AND g.boundingBoxWest >= :west)")
    List<OutageGeospatialData> findOutagesInBoundingBox(
            @Param("north") Double north,
            @Param("south") Double south,
            @Param("east") Double east,
            @Param("west") Double west);
}