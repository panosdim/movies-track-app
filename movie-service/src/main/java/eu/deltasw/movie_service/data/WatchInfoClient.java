package eu.deltasw.movie_service.data;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import eu.deltasw.common.model.dto.WatchInfoRequest;
import eu.deltasw.common.model.dto.WatchInfoResponse;

@FeignClient(name = "tmdb-service")
public interface WatchInfoClient {
    @GetMapping("/watch-info")
    public List<WatchInfoResponse> getWatchInfo(@RequestBody WatchInfoRequest movies);
}
