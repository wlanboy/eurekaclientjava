package com.example.eurekaclient.actuator;

import com.example.eurekaclient.services.LifecycleManager;
import com.example.eurekaclient.services.ServiceInstanceStore;
import com.example.eurekaclient.services.StartupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Endpoint(id = "refresh")
public class RefreshEndpoint {

    private static final Logger log = LoggerFactory.getLogger(RefreshEndpoint.class);

    private final LifecycleManager lifecycleManager;
    private final ServiceInstanceStore serviceInstanceStore;
    private final StartupService startupService;

    public RefreshEndpoint(LifecycleManager lifecycleManager,
                           ServiceInstanceStore serviceInstanceStore,
                           StartupService startupService) {
        this.lifecycleManager = lifecycleManager;
        this.serviceInstanceStore = serviceInstanceStore;
        this.startupService = startupService;
    }

    @WriteOperation
    public Map<String, Object> refresh() {
        log.info("[Refresh] Starte Refresh aller Service-Instanzen...");

        try {
            int stoppedCount = lifecycleManager.getRunningInstances().size();
            lifecycleManager.stopAllRunning();
            log.info("[Refresh] {} Instanzen gestoppt", stoppedCount);

            serviceInstanceStore.clear();
            log.info("[Refresh] ServiceInstanceStore geleert");

            startupService.loadAndImportServices();
            int loadedCount = serviceInstanceStore.getInstances().size();
            log.info("[Refresh] {} Instanzen aus services.json geladen", loadedCount);

            startupService.startAllLifecycles(lifecycleManager);
            int startedCount = lifecycleManager.getRunningInstances().size();
            log.info("[Refresh] {} Instanzen gestartet", startedCount);

            return Map.of(
                    "status", "success",
                    "stopped", stoppedCount,
                    "loaded", loadedCount,
                    "started", startedCount
            );

        } catch (Exception e) {
            log.error("[Refresh] Fehler beim Refresh: {}", e.getMessage(), e);
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }
    }
}
