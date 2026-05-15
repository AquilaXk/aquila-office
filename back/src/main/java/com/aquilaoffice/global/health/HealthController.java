package com.aquilaoffice.global.health;

import java.time.Instant;
import com.aquilaoffice.global.response.RsData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public RsData<HealthResponse> health() {
        return RsData.ok(new HealthResponse("ok", "aquila-office-back", Instant.now()));
    }

    public record HealthResponse(String status, String service, Instant checkedAt) {}
}
