package lk.ijse.poweralert.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.util.Properties;

@Configuration
public class FreemarkerConfig {

    private static final Logger logger = LoggerFactory.getLogger(FreemarkerConfig.class);

    @Bean
    public FreeMarkerConfigurer freeMarkerConfigurer() {
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        configurer.setTemplateLoaderPath("classpath:/templates/");
        configurer.setDefaultEncoding("UTF-8");

        // Configure additional settings for FreeMarker
        Properties settings = new Properties();
        settings.setProperty("template_exception_handler", "rethrow");
        settings.setProperty("default_encoding", "UTF-8");
        settings.setProperty("number_format", "computer");
        configurer.setFreemarkerSettings(settings);

        // Log the template path to help debug
        try {
            ClassPathResource resource = new ClassPathResource("templates/outage-notification.ftl");
            logger.info("Template exists at path: {}", resource.exists());
        } catch (Exception e) {
            logger.error("Error checking template path: {}", e.getMessage());
        }

        return configurer;
    }
}