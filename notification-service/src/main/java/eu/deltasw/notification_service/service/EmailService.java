package eu.deltasw.notification_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import eu.deltasw.common.model.dto.MovieNotifyRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService implements NotificationService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${tmdb.image.base-url}")
    private String imageBaseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @Override
    public void sendNotification(MovieNotifyRequest request) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
                helper.setTo(request.getUserIds().toArray(new String[0]));
            }
            helper.setSubject("Movie Notification");

            // Use imageBaseUrl from application.yml
            String imageUrl = imageBaseUrl + request.getMoviePoster();
            String htmlMsg = "<h2>Watch providers updated for movie: " + request.getMovieTitle() + "</h2>"
                    + "<img src='" + imageUrl + "' alt='Movie Image' />";
            helper.setText(htmlMsg, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
