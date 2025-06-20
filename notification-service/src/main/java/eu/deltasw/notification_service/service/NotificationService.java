package eu.deltasw.notification_service.service;

import eu.deltasw.common.model.dto.MovieNotifyRequest;

public interface NotificationService {
    void sendNotification(MovieNotifyRequest request);
}
