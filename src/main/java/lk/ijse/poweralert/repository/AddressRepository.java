package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /** Find addresses by user ID ordered by primary flag    */
    List<Address> findByUserIdOrderByIsPrimaryDesc(Long userId);

    /** Find address by ID and user ID */
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    /** Find addresses by user ID excluding a specific address   */
    List<Address> findByUserIdAndIdNot(Long userId, Long addressId);

    /** Check if an address exists for the user  */
    boolean existsByIdAndUserId(Long id, Long userId);
}