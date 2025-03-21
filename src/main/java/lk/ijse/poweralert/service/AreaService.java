package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.AreaDTO;
import lk.ijse.poweralert.dto.UtilityProviderDTO;

import java.util.List;

public interface AreaService {

    /**
     * Get all areas
     * @return list of all areas
     */
    List<AreaDTO> getAllAreas();

    /**
     * Get area by ID
     * @param id the area ID
     * @return the area with the given ID
     */
    AreaDTO getAreaById(Long id);

    /**
     * Create a new area
     * @param areaDTO the area to create
     * @return the created area
     */
    AreaDTO createArea(AreaDTO areaDTO);

    /**
     * Update an existing area
     * @param areaDTO the area to update
     * @return the updated area
     */
    AreaDTO updateArea(AreaDTO areaDTO);

    /**
     * Delete an area
     * @param id the ID of the area to delete
     * @return true if the area was deleted successfully
     */
    boolean deleteArea(Long id);

    /**
     * Get utility providers for an area
     * @param areaId the area ID
     * @return list of utility providers for the area
     */
    List<UtilityProviderDTO> getUtilityProvidersForArea(Long areaId);

    /**
     * Link a utility provider to an area
     * @param areaId the area ID
     * @param providerId the utility provider ID
     * @return the updated area
     */
    AreaDTO linkUtilityProviderToArea(Long areaId, Long providerId);

    /**
     * Unlink a utility provider from an area
     * @param areaId the area ID
     * @param providerId the utility provider ID
     * @return the updated area
     */
    AreaDTO unlinkUtilityProviderFromArea(Long areaId, Long providerId);
}