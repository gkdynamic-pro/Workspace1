package net.codejava.config;

import net.codejava.model.AppUser;
import net.codejava.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedConfig {

    @Bean
    CommandLineRunner seedAdmin(AppUserRepository repo, PasswordEncoder encoder) {
        return args -> {
            String uname = "admin";
            if (!repo.existsByUsername(uname)) {
                AppUser admin = new AppUser();
                admin.setUsername(uname);
                admin.setPassword(encoder.encode("Admin@123"));
                admin.getRoles().add("ADMIN");
                admin.getRoles().add("USER");
                repo.save(admin);
                System.out.println(">> Seeded default admin: admin / Admin@123");
            }
        };
    }
}
