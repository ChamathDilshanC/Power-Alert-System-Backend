package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.enums.AppEnums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by username
     * @param username the username
     * @return the user with the given username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by email
     * @param email the email
     * @return the user with the given email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email
     * @param email the email
     * @return true if a user exists with the given email
     */
    boolean existsByEmail(String email);

    /**
     * Check if a user exists with the given username
     * @param username the username
     * @return true if a user exists with the given username
     */
    boolean existsByUsername(String username);

    /**
     * Find users by role
     * @param role the role
     * @return list of users with the given role
     */
    List<User> findByRole(Role role);

    /**
     * Find users by district
     * @param district the district
     * @return list of users with addresses in the given district
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.addresses a WHERE a.district = :district AND u.isActive = true")
    List<User> findUsersInDistrict(@Param("district") String district);

    /**
     * Find users by geographical area
     * This is a placeholder for future geospatial query when PostGIS is integrated
     * @param areaId the area ID
     * @return list of users with addresses in the given area
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.addresses a JOIN Area area WHERE area.id = :areaId AND a.district = area.district AND u.isActive = true")
    List<User> findUsersByAreaId(@Param("areaId") Long areaId);
}