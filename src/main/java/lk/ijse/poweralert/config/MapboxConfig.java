package lk.ijse.poweralert.config;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.staticmap.v1.MapboxStaticMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;

@Configuration
@PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true)
public class MapboxConfig {
    private static final Logger logger = LoggerFactory.getLogger(MapboxConfig.class);

    @Value("${mapbox.api-key:pk.dummy.key}")
    private String mapboxApiKey;

    @Value("${mapbox.static-map.width:800}")
    private int defaultMapWidth;

    @Value("${mapbox.static-map.height:600}")
    private int defaultMapHeight;

    @Value("${mapbox.country:lk}")
    private String defaultCountry;

    @PostConstruct
    public void init() {
        if ("pk.dummy.key".equals(mapboxApiKey)) {
            logger.warn("No Mapbox API key configured. Geospatial features will be limited.");
        } else {
            logger.info("Mapbox configuration initialized with default map size: {}x{}",
                    defaultMapWidth, defaultMapHeight);
        }
    }

    /**
     * Creates a MapboxGeocoding.Builder bean for dependency injection
     * @return MapboxGeocoding.Builder
     */
    @Bean
    public MapboxGeocoding.Builder mapboxGeocodingBuilder() {
        return MapboxGeocoding.builder()
                .accessToken(mapboxApiKey)
                .country(defaultCountry); // Focus geocoding on Sri Lanka by default
    }

    /**
     * Creates a MapboxStaticMap.Builder bean for dependency injection
     * @return MapboxStaticMap.Builder
     */
    @Bean
    public MapboxStaticMap.Builder mapboxStaticMapBuilder() {
        return MapboxStaticMap.builder()
                .accessToken(mapboxApiKey);
    }

    /**
     * @return Default map width from configuration
     */
    @Bean
    public int defaultMapWidth() {
        return defaultMapWidth;
    }

    /**
     * @return Default map height from configuration
     */
    @Bean
    public int defaultMapHeight() {
        return defaultMapHeight;
    }
}