package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.AlternativeResourceDTO;
import lk.ijse.poweralert.dto.ResourceImageDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AlternativeResourceService {
    // Existing methods
    List<AlternativeResourceDTO> getAllResources();
    AlternativeResourceDTO getResourceById(Long id);
    List<AlternativeResourceDTO> getResourcesByArea(Long areaId);
    AlternativeResourceDTO addResource(AlternativeResourceDTO resourceDTO);
    AlternativeResourceDTO updateResource(AlternativeResourceDTO resourceDTO);
    boolean deleteResource(Long id);

    // New methods for image handling
    AlternativeResourceDTO uploadResourceImage(Long resourceId, MultipartFile file) throws IOException;
    ResourceImageDTO getResourceImage(Long resourceId);
    boolean deleteResourceImage(Long resourceId);
}