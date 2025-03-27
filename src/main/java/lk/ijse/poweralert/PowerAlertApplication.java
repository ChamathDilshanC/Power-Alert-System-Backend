package lk.ijse.poweralert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PowerAlertApplication {

    public static void main(String[] args) {
        SpringApplication.run(PowerAlertApplication.class, args);
    }

}
