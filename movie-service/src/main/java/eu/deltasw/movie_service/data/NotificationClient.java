package eu.deltasw.movie_service.data;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import eu.deltasw.common.model.dto.MovieNotifyRequest;

@FeignClient(name = "notification-service")
public interface NotificationClient {
    @PostMapping("/notify")
    public Void notify(@RequestBody MovieNotifyRequest notifyRequest);
}
