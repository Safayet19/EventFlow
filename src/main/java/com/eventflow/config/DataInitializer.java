package com.eventflow.config;

import com.eventflow.model.Event;
import com.eventflow.model.EventImage;
import com.eventflow.model.Feedback;
import com.eventflow.model.Registration;
import com.eventflow.model.User;
import com.eventflow.repository.EventImageRepository;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.FeedbackRepository;
import com.eventflow.repository.RegistrationRepository;
import com.eventflow.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner loadDemoData(EventRepository eventRepository,
                                   EventImageRepository eventImageRepository,
                                   RegistrationRepository registrationRepository,
                                   FeedbackRepository feedbackRepository,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${eventflow.demo-seed.enabled:true}") boolean demoSeedEnabled) {
        return args -> {
            if (demoSeedEnabled && eventRepository.count() == 0) {
                LocalDate today = LocalDate.now();
                eventRepository.saveAll(List.of(
                        Event.builder()
                                .title("Tech Innovation Summit")
                                .category("Technology")
                                .location("Dhaka Convention Center")
                                .date(today.plusDays(5))
                                .time(LocalTime.of(10, 0))
                                .description("A full-day gathering for technology, startup and innovation enthusiasts.")
                                .capacity(450)
                                .status("UPCOMING")
                                .featured(true)
                                .organizerName("EventFlow Team").build(),
                        Event.builder()
                                .title("Creative Design Meetup")
                                .category("Design")
                                .location("Banani Creative Hub")
                                .date(today.plusDays(8))
                                .time(LocalTime.of(17, 30))
                                .description("Meet designers, creators and students for talks, networking and portfolio exchange.")
                                .capacity(180)
                                .status("UPCOMING")
                                .featured(true)
                                .organizerName("Pixel Society").build(),
                        Event.builder()
                                .title("Campus Music Night")
                                .category("Music")
                                .location("University Auditorium")
                                .date(today.plusDays(12))
                                .time(LocalTime.of(18, 30))
                                .description("An energetic evening of live music, student performances and community celebration.")
                                .capacity(700)
                                .status("UPCOMING")
                                .featured(true)
                                .organizerName("Campus Culture Club").build(),
                        Event.builder()
                                .title("Startup Founders Circle")
                                .category("Business")
                                .location("Gulshan Business Lounge")
                                .date(today.plusDays(15))
                                .time(LocalTime.of(15, 0))
                                .description("A focused networking session for founders, aspiring entrepreneurs and mentors.")
                                .capacity(120)
                                .status("UPCOMING")
                                .featured(false)
                                .organizerName("LaunchPad").build(),
                        Event.builder()
                                .title("Photography Walk")
                                .category("Community")
                                .location("Hatirjheel")
                                .date(today.plusDays(18))
                                .time(LocalTime.of(7, 0))
                                .description("A relaxed city photo walk for beginners and experienced photographers.")
                                .capacity(90)
                                .status("UPCOMING")
                                .featured(false)
                                .organizerName("Dhaka Frames").build(),
                        Event.builder()
                                .title("Career & Skills Expo")
                                .category("Career")
                                .location("International Convention City")
                                .date(today.plusDays(24))
                                .time(LocalTime.of(9, 30))
                                .description("Explore career opportunities, workshops and practical skill sessions in one place.")
                                .capacity(1000)
                                .status("UPCOMING")
                                .featured(false)
                                .organizerName("FutureWorks").build()
                ));
            }

            migrateLegacyInlineImages(eventRepository, eventImageRepository);

            // Seed missing demo accounts on localhost and Render alike.
            // Existing accounts, credentials and profile changes are never overwritten.
            if (demoSeedEnabled) {
                ensureDemoUser(userRepository, passwordEncoder,
                        "Safayet Ullah", "admin@eventflow.com", "Admin123!", "ADMIN");
                User demoOrganizer = ensureDemoUser(userRepository, passwordEncoder,
                        "Safayet Ullah", "organizer@eventflow.com", "Organizer123!", "ORGANIZER");
                User demoParticipant = ensureDemoUser(userRepository, passwordEncoder,
                        "Safayet Ullah", "participant@eventflow.com", "Participant123!", "PARTICIPANT");
                syncDemoDisplayNames(eventRepository, registrationRepository, feedbackRepository,
                        demoOrganizer, demoParticipant);
            }

        };
    }

    private User ensureDemoUser(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                String fullName,
                                String email,
                                String plainPassword,
                                String role) {
        // Never overwrite a changed password, role, status or profile on restart.
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            return userRepository.save(User.builder()
                    .fullName(fullName)
                    .email(email)
                    .password(passwordEncoder.encode(plainPassword))
                    .role(role)
                    .enabled(true)
                    .build());
        });
    }

    private void syncDemoDisplayNames(EventRepository eventRepository,
                                      RegistrationRepository registrationRepository,
                                      FeedbackRepository feedbackRepository,
                                      User demoOrganizer,
                                      User demoParticipant) {
        List<Event> organizerEvents = eventRepository.findByOrganizerIdOrderByDateAsc(demoOrganizer.getId());
        organizerEvents.forEach(event -> event.setOrganizerName(demoOrganizer.getFullName()));
        if (!organizerEvents.isEmpty()) {
            eventRepository.saveAll(organizerEvents);
        }

        List<Registration> participantRegistrations =
                registrationRepository.findByParticipantIdOrderByRegisteredAtDesc(demoParticipant.getId());
        participantRegistrations.forEach(registration ->
                registration.setParticipantName(demoParticipant.getFullName()));
        if (!participantRegistrations.isEmpty()) {
            registrationRepository.saveAll(participantRegistrations);
        }

        List<Feedback> participantFeedback =
                feedbackRepository.findByParticipantIdOrderByUpdatedAtDesc(demoParticipant.getId());
        participantFeedback.forEach(item -> item.setParticipantName(demoParticipant.getFullName()));
        if (!participantFeedback.isEmpty()) {
            feedbackRepository.saveAll(participantFeedback);
        }
    }

    private void migrateLegacyInlineImages(EventRepository eventRepository,
                                           EventImageRepository eventImageRepository) {
        for (Event event : eventRepository.findAll()) {
            String imageUrl = event.getImageUrl();
            if (imageUrl == null || !imageUrl.startsWith("data:")) {
                continue;
            }

            int comma = imageUrl.indexOf(',');
            int semicolon = imageUrl.indexOf(';');
            if (comma < 0 || semicolon < 5 || semicolon > comma) {
                continue;
            }

            try {
                String contentType = imageUrl.substring(5, semicolon);
                byte[] data = Base64.getDecoder().decode(imageUrl.substring(comma + 1));
                eventImageRepository.deleteByEventId(event.getId());
                EventImage storedImage = eventImageRepository.save(EventImage.builder().eventId(event.getId()).contentType(contentType).data(data).build());
                event.setImageUrl("/event-images/" + event.getId() + "?v=" + storedImage.getId());
                eventRepository.save(event);
            } catch (IllegalArgumentException ignored) {
                // Keep the old image value if an old data URL is malformed.
            }
        }
    }
}
