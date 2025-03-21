package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.dto.OutageDTO;
import lk.ijse.poweralert.dto.UtilityProviderDTO;
import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.entity.UtilityProvider;
import lk.ijse.poweralert.enums.AppEnums.Role;
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
import java.util.List;
import java.util.Optional;
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

    @Override
    public List<UtilityProviderDTO> getAllUtilityProviders() {
        logger.info("Fetching all utility providers");

        List<UtilityProvider> providers = utilityProviderRepository.findAll();

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

        // Verify provider exists
        UtilityProvider provider = utilityProviderRepository.findById(utilityProviderDTO.getId())
                .orElseThrow(() -> new EntityNotFoundException("Utility provider not found with ID: " + utilityProviderDTO.getId()));

        // Update fields
        provider.setName(utilityProviderDTO.getName());
        provider.setContactEmail(utilityProviderDTO.getContactEmail());
        provider.setContactPhone(utilityProviderDTO.getContactPhone());
        provider.setWebsite(utilityProviderDTO.getWebsite());
        // Don't update the type as it's a fundamental property

        // Save updated provider
        UtilityProvider updatedProvider = utilityProviderRepository.save(provider);
        logger.info("Utility provider updated with ID: {}", updatedProvider.getId());

        return convertToDTO(updatedProvider);
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
     * Convert UtilityProvider entity to DTO
     */
    private UtilityProviderDTO convertToDTO(UtilityProvider provider) {
        return modelMapper.map(provider, UtilityProviderDTO.class);
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
            // For utility provider users, we need to determine which provider they're associated with

            // Option 1: If User has a direct reference to UtilityProvider entity
            // Uncomment this if you have this relationship set up
            /*
            if (user.getUtilityProvider() != null) {
                return user.getUtilityProvider().getId();
            }
            */

            // Option 2: Look up the provider based on some criteria
            // This is a placeholder - replace with your actual logic
            List<UtilityProvider> allProviders = utilityProviderRepository.findAll();

            // For demonstration, let's find a provider with a name or email that matches the user's username or email
            Optional<UtilityProvider> matchingProvider = allProviders.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(user.getUsername()) ||
                            (p.getContactEmail() != null && p.getContactEmail().equalsIgnoreCase(user.getEmail())))
                    .findFirst();

            if (matchingProvider.isPresent()) {
                return matchingProvider.get().getId();
            }

            // Fallback: Just use the first provider if we can't find a match
            // In production, you should have a proper mapping
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
}