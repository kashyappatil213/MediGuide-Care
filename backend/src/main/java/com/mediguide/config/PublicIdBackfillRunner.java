package com.mediguide.config;

import com.mediguide.model.User;
import com.mediguide.repository.UserRepository;
import com.mediguide.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Startup backfill runner to ensure any existing users created prior to the public ID migration
 * are safely populated with unique, role-prefixed public IDs.
 */
@Component
public class PublicIdBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PublicIdBackfillRunner.class);

    private final UserRepository userRepository;
    private final AuthService authService;

    public PublicIdBackfillRunner(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<User> usersWithoutPublicId = userRepository.findByPublicIdIsNull();
        if (usersWithoutPublicId != null && !usersWithoutPublicId.isEmpty()) {
            log.info("Found {} existing users without public IDs. Initiating safe backfill...", usersWithoutPublicId.size());
            for (User user : usersWithoutPublicId) {
                if (user.getRole() != null) {
                    String uniquePublicId = authService.generateUniquePublicId(user.getRole());
                    user.setPublicId(uniquePublicId);
                    userRepository.save(user);
                }
            }
            log.info("Completed public ID backfill for {} users.", usersWithoutPublicId.size());
        }
    }
}
