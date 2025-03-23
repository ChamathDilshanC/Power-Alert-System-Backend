package lk.ijse.poweralert.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.util.Properties;

@Configuration
public class FreemarkerConfig {

    private static final Logger logger = LoggerFactory.getLogger(FreemarkerConfig.class);

    @Bean
    public FreeMarkerConfigurer freeMarkerConfigurer() {
        logger.info("Configuring FreeMarker");

        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();

        // Set template loader path
        configurer.setTemplateLoaderPath("classpath:/templates/");
        configurer.setDefaultEncoding("UTF-8");

        // Configure FreeMarker properties
        Properties settings = new Properties();
        settings.setProperty("template_exception_handler", "rethrow");
        settings.setProperty("default_encoding", "UTF-8");
        settings.setProperty("number_format", "computer");
        settings.setProperty("auto_import", "");
        settings.setProperty("whitespace_stripping", "true");

        configurer.setFreemarkerSettings(settings);

        logger.info("FreeMarker configured with template path: classpath:/templates/");
        return configurer;
    }
}