package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CloseWorkspacePresenceStreamUseCase;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * 활성 SSE presence 연결에 주기적 keepalive 주석을 보내고 끊어진 유령 sink를 정리한다.
 */
@Component
public class WorkspacePresenceHeartbeatScheduler {

    private static final long HEARTBEAT_INTERVAL_SECONDS = 25L;
    private static final Duration STALE_SINK_DURATION = Duration.ofMinutes(15);

    private final CloseWorkspacePresenceStreamUseCase closeWorkspacePresenceStreamUseCase;
    private final ScheduledExecutorService executor;

    public WorkspacePresenceHeartbeatScheduler(
            CloseWorkspacePresenceStreamUseCase closeWorkspacePresenceStreamUseCase) {
        this.closeWorkspacePresenceStreamUseCase = Objects.requireNonNull(
                closeWorkspacePresenceStreamUseCase,
                "closeWorkspacePresenceStreamUseCase must not be null");
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "aquila-presence-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        this.executor.scheduleWithFixedDelay(
                this::runPulseSafely,
                HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }

    private void runPulseSafely() {
        try {
            closeWorkspacePresenceStreamUseCase.pulseHeartbeatsAndPruneStale(STALE_SINK_DURATION);
        } catch (RuntimeException ignored) {
            // heartbeat 실패가 스케줄러 스레드를 중단시키지 않도록 방어한다.
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
