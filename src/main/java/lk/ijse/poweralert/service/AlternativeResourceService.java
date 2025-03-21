package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.AlternativeResourceDTO;

import java.util.List;

public interface AlternativeResourceService {

    /**
     * Get all alternative resources
     * @return list of all alternative resources
     */
    List<AlternativeResourceDTO> getAllResources();

    /**
     * Get alternative resource by ID
     * @param id the resource ID
     * @return the alternative resource
     */
    AlternativeResourceDTO getResourceById(Long id);

    /**
     * Get alternative resources by area
     * @param areaId the area ID
     * @return list of alternative resources in the area
     */
    List<AlternativeResourceDTO> getResourcesByArea(Long areaId);

    /**
     * Add a new alternative resource
     * @param resourceDTO the resource to add
     * @return the saved alternative resource
     */
    AlternativeResourceDTO addResource(AlternativeResourceDTO resourceDTO);

    /**
     * Update an existing alternative resource
     * @param resourceDTO the resource to update
     * @return the updated alternative resource
     */
    AlternativeResourceDTO updateResource(AlternativeResourceDTO resourceDTO);

    /**
     * Delete an alternative resource
     * @param id the ID of the resource to delete
     * @return true if the resource was deleted successfully
     */
    boolean deleteResource(Long id);
}