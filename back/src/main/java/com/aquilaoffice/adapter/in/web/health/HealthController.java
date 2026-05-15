package com.aquilaoffice.adapter.in.web.health;

import com.aquilaoffice.global.response.RsData;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로드밸런서와 운영자가 백엔드 상태를 확인하는 health check용 web adapter다.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public RsData<HealthResponse> health() {
        return RsData.ok(new HealthResponse("ok", "aquila-office-back", Instant.now()));
    }

    /**
     * health check 응답에 필요한 서비스 상태와 확인 시각을 담는다.
     */
    public record HealthResponse(String status, String service, Instant checkedAt) {}
}
