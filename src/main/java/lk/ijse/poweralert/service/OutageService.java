package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.OutageCreateDTO;
import lk.ijse.poweralert.dto.OutageDTO;
import lk.ijse.poweralert.dto.OutageUpdateDTO;

import java.util.List;

public interface OutageService {

    /** Create a new outage  */
    OutageDTO createOutage(OutageCreateDTO outageCreateDTO);

    /** Get all active outages */
    List<OutageDTO> getAllActiveOutages();

    /** Get outage by ID */
    OutageDTO getOutageById(Long id);

    /** Get outages for a specific area  */
    List<OutageDTO> getOutagesByArea(Long areaId);

    /** Update an existing outage    */
    OutageDTO updateOutage(Long id, OutageCreateDTO outageCreateDTO);

    /** Add an update to an existing outage  */
    OutageDTO addOutageUpdate(OutageUpdateDTO outageUpdateDTO);

    /** Cancel an outage */
    OutageDTO cancelOutage(Long id);

    /** Get outages relevant to the current authenticated user */
    List<OutageDTO> getOutagesForCurrentUser();
}