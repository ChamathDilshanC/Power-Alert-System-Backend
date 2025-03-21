package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.OutageDTO;
import lk.ijse.poweralert.dto.UtilityProviderDTO;

import java.util.List;

public interface UtilityProviderService {

    /** Get all utility providers*/
    List<UtilityProviderDTO> getAllUtilityProviders();

    /** Get utility provider by ID   */
    UtilityProviderDTO getUtilityProviderById(Long id);

    /** Update utility provider details  */
    UtilityProviderDTO updateUtilityProvider(UtilityProviderDTO utilityProviderDTO);

    /** Get outages for the current utility provider */
    List<OutageDTO> getOutagesForCurrentProvider();

    /** Create a new utility provider and link to a user */
    UtilityProviderDTO createUtilityProvider(UtilityProviderDTO utilityProviderDTO, Long userId);
}