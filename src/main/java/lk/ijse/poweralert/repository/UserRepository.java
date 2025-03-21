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


    /** Find a user by username  */
    Optional<User> findByUsername(String username);

    /** Find a user by email */
    Optional<User> findByEmail(String email);

    /** Check if a user exists with the given email */
    boolean existsByEmail(String email);

    /** Check if a user exists with the given username  */
    boolean existsByUsername(String username);

    /** Find users by role */
    List<User> findByRole(Role role);

    /** Find all active users    */
    List<User> findByIsActiveTrue();

    /** Find users by district */
    @Query("SELECT DISTINCT u FROM User u JOIN u.addresses a WHERE a.district = :district AND u.isActive = true")
    List<User> findUsersInDistrict(@Param("district") String district);

    @Query("SELECT DISTINCT u FROM User u JOIN u.addresses a JOIN Area area ON a.district = area.district " +
            "WHERE area.id = :areaId AND u.isActive = true")
    List<User> findUsersByAreaId(@Param("areaId") Long areaId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.addresses LEFT JOIN FETCH u.notificationPreferences WHERE u.username = :username")
    Optional<User> findByUsernameWithCollections(@Param("username") String username);
}