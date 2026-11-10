package com.eventmanager.config;

import com.eventmanager.model.AppUser;
import com.eventmanager.model.Booking;
import com.eventmanager.model.EventForm;
import com.eventmanager.repository.EventRepository;
import com.eventmanager.repository.PaymentRepository;
import com.eventmanager.repository.UserRepository;
import com.eventmanager.service.BookingService;
import com.eventmanager.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Creates demo accounts and events the first time the app starts on an empty database. */
@Component
@ConditionalOnProperty(name = "app.seed-sample-data", havingValue = "true")
public class SampleDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataSeeder.class);

    private final UserRepository users;
    private final EventRepository events;
    private final PaymentRepository payments;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PasswordEncoder encoder;

    public SampleDataSeeder(UserRepository users, EventRepository events, PaymentRepository payments,
                            BookingService bookingService, PaymentService paymentService, PasswordEncoder encoder) {
        this.users = users;
        this.events = events;
        this.payments = payments;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.countAll() > 0) return;

        users.create("Site Admin", "admin@example.com", "9000000001", encoder.encode("Admin@123"), "ADMIN", "NONE", true);
        long organizerId = users.create("Olivia Organizer", "organizer@example.com", "9000000002",
                encoder.encode("Organizer@123"), "ORGANIZER", "APPROVED", true);
        long attendeeId = users.create("Arjun Attendee", "user@example.com", "9000000003",
                encoder.encode("User@12345"), "USER", "NONE", true);
        AppUser attendee = users.findById(attendeeId).orElseThrow();

        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        long workshop = publish(organizerId, "Java Backend Workshop", "Workshop", "Meerut", "Lab 3, CS Block", null,
                now.minusHours(1), now.plusHours(3), 40, "0", 24,
                "Hands-on session building REST APIs with Spring Boot and JDBC. Bring a laptop with IntelliJ IDEA.");
        long bootcamp = publish(organizerId, "Spring Boot Bootcamp", "Workshop", "Delhi", "Tech Hub, Connaught Place", null,
                now.plusDays(3), now.plusDays(3).plusHours(6), 60, "499", 24,
                "A full day on Spring Boot, Spring Security and JDBC with a mini project.");
        long hackathon = publish(organizerId, "Campus Hackathon 2026", "Conference", "Meerut", "Main Auditorium", null,
                now.plusDays(5), now.plusDays(6), 120, "0", 24,
                "24 hours, teams of four, one problem statement revealed at the start.");
        long music = publish(organizerId, "Indie Music Night", "Concert", "Mumbai", "Blue Frog, Lower Parel", null,
                now.plusDays(9), now.plusDays(9).plusHours(4), 200, "799", 48,
                "Three indie bands, one stage. Full refund until 48 hours before the show.");
        publish(organizerId, "Product Design Webinar", "Webinar", null, null, "https://meet.example.com/design",
                now.plusDays(2), now.plusDays(2).plusHours(1), 500, "0", 0,
                "How small teams ship better interfaces. Link is shown on your ticket.");
        publish(organizerId, "Startup Pitch Day", "Conference", "Bengaluru", "Convention Centre, Whitefield", null,
                now.plusDays(20), now.plusDays(20).plusHours(8), 300, "1499", 72,
                "Twenty early-stage startups pitch to a panel of investors.");

        EventForm draft = form("Winter Meetup (draft)", "Meetup", "Delhi", "Community Hall", null,
                now.plusDays(40), now.plusDays(40).plusHours(2), 30, "0", 24, "Not published yet.");
        events.create(organizerId, draft, null, "DRAFT");

        // A few bookings so the dashboards, tickets and reports have something to show.
        bookingService.book(attendee, workshop, 1);
        bookingService.book(attendee, hackathon, 2);
        payPaidBooking(attendee, bootcamp, 2);
        payPaidBooking(attendee, music, 1);

        log.info("""

                ======== Demo accounts (password in brackets) ========
                admin@example.com      [Admin@123]
                organizer@example.com  [Organizer@123]
                user@example.com       [User@12345]
                ======================================================
                """);
    }

    /** Books a paid event and completes the payment the way the gateway callback would. */
    private void payPaidBooking(AppUser user, long eventId, int quantity) {
        Booking booking = bookingService.book(user, eventId, quantity);
        String orderId = payments.findByBooking(booking.id()).orElseThrow().orderId();
        paymentService.confirmVerified(orderId, "pay_seed_" + booking.ref());
    }

    private long publish(long organizerId, String title, String category, String city, String venue, String link,
                         LocalDateTime start, LocalDateTime end, int capacity, String price, int refundHours,
                         String description) {
        EventForm f = form(title, category, city, venue, link, start, end, capacity, price, refundHours, description);
        return events.create(organizerId, f, null, "PUBLISHED");
    }

    private EventForm form(String title, String category, String city, String venue, String link,
                           LocalDateTime start, LocalDateTime end, int capacity, String price, int refundHours,
                           String description) {
        EventForm f = new EventForm();
        f.setTitle(title);
        f.setCategory(category);
        f.setCity(city);
        f.setVenue(venue);
        f.setOnlineLink(link);
        f.setStartTime(start);
        f.setEndTime(end);
        f.setCapacity(capacity);
        f.setPrice(new BigDecimal(price));
        f.setRefundCutoffHours(refundHours);
        f.setDescription(description);
        return f;
    }
}
