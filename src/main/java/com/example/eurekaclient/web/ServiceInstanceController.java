package com.example.eurekaclient.web;

import com.example.eurekaclient.services.LifecycleManager;
import com.example.eurekaclient.services.ServiceInstance;
import com.example.eurekaclient.services.UpdateInstanceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/instances")
@RequiredArgsConstructor
public class ServiceInstanceController {

    private final LifecycleManager lifecycleManager;

    @GetMapping("/configured")
    public ResponseEntity<List<ServiceInstance>> getConfiguredInstances() {
        return ResponseEntity.ok(lifecycleManager.getConfiguredInstances());
    }

    @GetMapping("/running")
    public ResponseEntity<List<ServiceInstance>> getRunningInstances() {
        return ResponseEntity.ok(lifecycleManager.getRunningInstances());
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateInstance(@RequestBody UpdateInstanceRequest request) {

        log.info("Update-Request für ServiceInstance: {}", request.getServiceName());

        ServiceInstance instance = lifecycleManager.updateInstance(request);

        if (instance == null) {
            log.warn("ServiceInstance '{}' nicht gefunden", request.getServiceName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("ServiceInstance '" + request.getServiceName() + "' nicht gefunden.");
        }

        log.info("ServiceInstance '{}' erfolgreich aktualisiert", request.getServiceName());
        return ResponseEntity.ok(
                "Instanz '" + request.getServiceName() + "' erfolgreich aktualisiert und neu gestartet."
        );
    }
}
