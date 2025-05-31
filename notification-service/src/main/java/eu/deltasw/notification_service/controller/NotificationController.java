package eu.deltasw.notification_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.deltasw.common.model.dto.MovieNotifyRequest;
import eu.deltasw.notification_service.service.NotificationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/")
public class NotificationController {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService emailService;

    public NotificationController(NotificationService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/notify")
    public ResponseEntity<?> notify(@Valid @RequestBody MovieNotifyRequest notifyRequest) {
        logger.info("Received notification request: {}", notifyRequest);
        // Run email sending async
        emailService.sendNotification(notifyRequest);
        return ResponseEntity.noContent().<Void>build();
    }
}
