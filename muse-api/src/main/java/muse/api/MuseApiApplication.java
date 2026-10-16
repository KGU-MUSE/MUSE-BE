package muse.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(scanBasePackages = {"muse.api", "muse.domain"}, exclude = UserDetailsServiceAutoConfiguration.class)
public class MuseApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MuseApiApplication.class, args);
    }
}
