package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.OutageCreateDTO;
import lk.ijse.poweralert.dto.OutageDTO;
import lk.ijse.poweralert.dto.OutageUpdateDTO;

import java.util.List;

public interface OutageService {

    /**
     * Create a new outage
     * @param outageCreateDTO the outage creation data
     * @return the created outage
     */
    OutageDTO createOutage(OutageCreateDTO outageCreateDTO);

    /**
     * Get all active outages
     * @return list of active outages
     */
    List<OutageDTO> getAllActiveOutages();

    /**
     * Get outage by ID
     * @param id the outage ID
     * @return the outage details
     */
    OutageDTO getOutageById(Long id);

    /**
     * Get outages for a specific area
     * @param areaId the area ID
     * @return list of outages for the area
     */
    List<OutageDTO> getOutagesByArea(Long areaId);

    /**
     * Update an existing outage
     * @param id the outage ID
     * @param outageCreateDTO the updated outage data
     * @return the updated outage
     */
    OutageDTO updateOutage(Long id, OutageCreateDTO outageCreateDTO);

    /**
     * Add an update to an existing outage
     * @param outageUpdateDTO the outage update data
     * @return the updated outage
     */
    OutageDTO addOutageUpdate(OutageUpdateDTO outageUpdateDTO);

    /**
     * Cancel an outage
     * @param id the outage ID
     * @return the cancelled outage
     */
    OutageDTO cancelOutage(Long id);

    /**
     * Get outages relevant to the current authenticated user
     * @return list of outages for the user's area
     */
    List<OutageDTO> getOutagesForCurrentUser();
}