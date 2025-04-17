package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.dto.AreaSummaryDTO;
import lk.ijse.poweralert.dto.OutageDTO;
import lk.ijse.poweralert.dto.UtilityProviderDTO;
import lk.ijse.poweralert.entity.Area;
import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.entity.UtilityProvider;
import lk.ijse.poweralert.enums.AppEnums.Role;
import lk.ijse.poweralert.repository.AreaRepository;
import lk.ijse.poweralert.repository.OutageRepository;
import lk.ijse.poweralert.repository.UtilityProviderRepository;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.UserService;
import lk.ijse.poweralert.service.UtilityProviderService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UtilityProviderServiceImpl implements UtilityProviderService {

    private static final Logger logger = LoggerFactory.getLogger(UtilityProviderServiceImpl.class);

    @Autowired
    private UtilityProviderRepository utilityProviderRepository;

    @Autowired
    private OutageRepository outageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AreaRepository areaRepository;

    @Override
    @Transactional(readOnly = true)  // Add the @Transactional annotation here
    public List<UtilityProviderDTO> getAllUtilityProviders() {
        logger.info("Fetching all utility providers");

        List<UtilityProvider> providers = utilityProviderRepository.findAll();

        // Use the join fetch query instead if possible
        // List<UtilityProvider> providers = utilityProviderRepository.findAllWithServiceAreas();

        return providers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UtilityProviderDTO getUtilityProviderById(Long id) {
        logger.info("Fetching utility provider with ID: {}", id);

        UtilityProvider provider = utilityProviderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utility provider not found with ID: " + id));

        return convertToDTO(provider);
    }

    @Override
    @Transactional
    public UtilityProviderDTO updateUtilityProvider(UtilityProviderDTO utilityProviderDTO) {
        logger.info("Updating utility provider with ID: {}", utilityProviderDTO.getId());
        logger.debug("Update data received: {}", utilityProviderDTO);

        UtilityProvider provider = utilityProviderRepository.findById(utilityProviderDTO.getId())
                .orElseThrow(() -> new EntityNotFoundException("Utility provider not found with ID: " + utilityProviderDTO.getId()));

        // Update basic fields
        provider.setName(utilityProviderDTO.getName());
        provider.setType(utilityProviderDTO.getType());
        provider.setContactEmail(utilityProviderDTO.getContactEmail());
        provider.setContactPhone(utilityProviderDTO.getContactPhone());
        provider.setWebsite(utilityProviderDTO.getWebsite());

        // Handle service areas
        if (utilityProviderDTO.getServiceAreaIds() != null) {
            logger.debug("Processing service area IDs: {}", utilityProviderDTO.getServiceAreaIds());

            // Initialize collections if needed
            if (provider.getServiceAreas() == null) {
                provider.setServiceAreas(new ArrayList<>());
            }

            // Get current areas for cleanup
            List<Area> currentAreas = new ArrayList<>(provider.getServiceAreas());

            // Remove all current associations
            for (Area area : currentAreas) {
                if (area.getUtilityProviders() != null) {
                    area.getUtilityProviders().remove(provider);
                }
                provider.getServiceAreas().remove(area);
            }

            // Add new associations if any
            if (!utilityProviderDTO.getServiceAreaIds().isEmpty()) {
                List<Area> areasToAdd = areaRepository.findAllById(utilityProviderDTO.getServiceAreaIds());

                for (Area area : areasToAdd) {
                    // Initialize if needed
                    if (area.getUtilityProviders() == null) {
                        area.setUtilityProviders(new ArrayList<>());
                    }

                    provider.getServiceAreas().add(area);
                    area.getUtilityProviders().add(provider);
                }
            }
        }

        // Save the provider with updated relationships
        UtilityProvider savedProvider = utilityProviderRepository.save(provider);

        logger.info("Utility provider updated successfully with ID: {}", savedProvider.getId());

        // Convert to DTO for response
        return convertToDTO(savedProvider);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutageDTO> getOutagesForCurrentProvider() {
        logger.info("Fetching outages for current utility provider");

        try {
            // Get current user
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.getUserEntityByUsername(username);

            if (user.getRole() != Role.UTILITY_PROVIDER && user.getRole() != Role.ADMIN) {
                logger.error("User {} does not have utility provider or admin role", username);
                throw new IllegalStateException("User does not have permission to access provider outages");
            }

            // Get provider ID associated with this user
            Long providerId = getProviderIdForUser(user);
            logger.debug("Found provider ID {} for user {}", providerId, username);

            // Get outages for this provider
            List<Outage> outages = outageRepository.findByUtilityProviderIdOrderByStartTimeDesc(providerId);
            logger.info("Found {} outages for provider ID {}", outages.size(), providerId);

            return outages.stream()
                    .map(this::convertOutageToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching outages for current provider: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public UtilityProviderDTO createUtilityProvider(UtilityProviderDTO utilityProviderDTO, Long userId) {
        logger.info("Creating new utility provider for user ID: {}", userId);

        // Verify the user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Verify the user has utility provider role
        if (user.getRole() != Role.UTILITY_PROVIDER) {
            throw new IllegalStateException("User must have UTILITY_PROVIDER role");
        }

        // Create the utility provider entity
        UtilityProvider provider = new UtilityProvider();
        provider.setName(utilityProviderDTO.getName());
        provider.setContactEmail(utilityProviderDTO.getContactEmail());
        provider.setContactPhone(utilityProviderDTO.getContactPhone());
        provider.setWebsite(utilityProviderDTO.getWebsite());
        provider.setType(utilityProviderDTO.getType());

        // Save the provider
        UtilityProvider savedProvider = utilityProviderRepository.save(provider);

        // Update the user with a reference to the provider if you have that relationship
        // user.setUtilityProvider(savedProvider);
        // userRepository.save(user);

        return convertToDTO(savedProvider);
    }

    /**
     * Convert UtilityProvider entity to DTO, breaking circular references
     */
    private UtilityProviderDTO convertToDTO(UtilityProvider provider) {
        UtilityProviderDTO dto = new UtilityProviderDTO();
        dto.setId(provider.getId());
        dto.setName(provider.getName());
        dto.setContactEmail(provider.getContactEmail());
        dto.setContactPhone(provider.getContactPhone());
        dto.setWebsite(provider.getWebsite());
        dto.setType(provider.getType());

        // Use area summaries instead of full area DTOs to break circular references
        if (provider.getServiceAreas() != null) {
            dto.setServiceAreas(mapToAreaSummaries(provider.getServiceAreas()));

            // Set service area IDs for frontend use
            List<Long> serviceAreaIds = provider.getServiceAreas().stream()
                    .map(Area::getId)
                    .collect(Collectors.toList());
            dto.setServiceAreaIds(serviceAreaIds);
        }

        return dto;
    }

    /**
     * Maps areas to simplified AreaSummaryDTO objects to avoid circular references
     */
    private List<AreaSummaryDTO> mapToAreaSummaries(List<Area> areas) {
        if (areas == null) {
            return Collections.emptyList();
        }

        return areas.stream()
                .map(area -> AreaSummaryDTO.builder()
                        .id(area.getId())
                        .name(area.getName())
                        .district(area.getDistrict())
                        .province(area.getProvince())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Convert Outage entity to DTO
     */
    private OutageDTO convertOutageToDTO(Outage outage) {
        return modelMapper.map(outage, OutageDTO.class);
    }

    /**
     * Get provider ID for a user with ROLE_UTILITY_PROVIDER or ROLE_ADMIN
     */
    private Long getProviderIdForUser(User user) {
        if (user.getRole() == Role.ADMIN) {
            // Admin can access all providers, so we just get the first one for demonstrative purposes
            // In a real implementation, you might want to require the admin to select a specific provider
            List<UtilityProvider> providers = utilityProviderRepository.findAll();
            if (providers.isEmpty()) {
                throw new IllegalStateException("No utility providers found in the system");
            }
            return providers.get(0).getId();
        } else if (user.getRole() == Role.UTILITY_PROVIDER) {
            List<UtilityProvider> allProviders = utilityProviderRepository.findAll();

            // For demonstration, let's find a provider with a name or email that matches the user's username or email
            Optional<UtilityProvider> matchingProvider = allProviders.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(user.getUsername()) ||
                            (p.getContactEmail() != null && p.getContactEmail().equalsIgnoreCase(user.getEmail())))
                    .findFirst();

            if (matchingProvider.isPresent()) {
                return matchingProvider.get().getId();
            }

            if (!allProviders.isEmpty()) {
                logger.warn("No direct mapping found between user {} and a utility provider. Using the first available provider.",
                        user.getUsername());
                return allProviders.get(0).getId();
            }

            throw new IllegalStateException("No utility provider found for user: " + user.getUsername());
        } else {
            throw new IllegalStateException("User role not authorized to access utility provider information");
        }
    }

    @Transactional
    public int linkProviderToAreas(Long providerId, List<Long> areaIds) {
        logger.debug("Linking utility provider {} to service areas: {}", providerId, areaIds);

        if (areaIds == null || areaIds.isEmpty()) {
            return 0;
        }

        // Get utility provider
        UtilityProvider provider = utilityProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Utility provider not found with ID: " + providerId));

        // Get areas to link - using the autowired repository instance
        List<Area> areasToLink = areaRepository.findAllById(areaIds);

        if (areasToLink.isEmpty()) {
            logger.warn("No areas found for the provided IDs: {}", areaIds);
            return 0;
        }

        // Check if provider already has service areas
        if (provider.getServiceAreas() == null) {
            provider.setServiceAreas(new ArrayList<>());
        }

        int linkedCount = 0;

        // Link each area
        for (Area area : areasToLink) {
            if (!provider.getServiceAreas().contains(area)) {
                provider.getServiceAreas().add(area);
                linkedCount++;

                // Update the area's providers list if needed
                if (area.getUtilityProviders() == null) {
                    area.setUtilityProviders(new ArrayList<>());
                }

                if (!area.getUtilityProviders().contains(provider)) {
                    area.getUtilityProviders().add(provider);
                }
            }
        }

        // Save the provider with updated service areas
        utilityProviderRepository.save(provider);

        // Log the result
        logger.info("Successfully linked utility provider {} to {} areas", providerId, linkedCount);

        return linkedCount;
    }

    // Add better error handling in deleteUtilityProvider
    @Override
    @Transactional
    public boolean deleteUtilityProvider(Long id) {
        logger.info("Deleting utility provider with ID: {}", id);

        try {
            // Check if utility provider exists
            UtilityProvider provider = utilityProviderRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Utility provider not found with ID: " + id));

            // Remove associations with areas to avoid constraint violations
            if (provider.getServiceAreas() != null) {
                provider.getServiceAreas().clear();
                utilityProviderRepository.save(provider);
            }

            // Check for related users and handle appropriately
            List<User> relatedUsers = userRepository.findByUtilityProviderId(id);
            if (!relatedUsers.isEmpty()) {
                // Either update users or throw exception based on your business rules
                for (User user : relatedUsers) {
                    user.setUtilityProvider(null);
                    userRepository.save(user);
                }
            }

            // Delete the utility provider
            utilityProviderRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting utility provider: {}", e.getMessage(), e);
            throw e;
        }
    }
}