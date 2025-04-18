package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.AddressDTO;

import java.util.List;

public interface AddressService {

    /** Add a new address for the current user   */
    AddressDTO addAddress(AddressDTO addressDTO);

    /** Get all addresses for the current user  */
    List<AddressDTO> getCurrentUserAddresses();

    /** Get address by ID for the current user  */
    AddressDTO getAddressById(Long id);

    /** Update an existing address  */
    AddressDTO updateAddress(Long id, AddressDTO addressDTO);

    /** Delete an address   */
    boolean deleteAddress(Long id);
}