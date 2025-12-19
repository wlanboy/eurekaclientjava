package com.example.eurekaclient.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.eurekaclient.services.LifecycleManager;
import com.example.eurekaclient.services.ServiceInstance;
import com.example.eurekaclient.services.UpdateInstanceRequest;

@RestController
@RequestMapping("/instances")
public class ServiceInstanceController {
    
    private static final Logger log = LoggerFactory.getLogger(ServiceInstanceController.class);

    private final LifecycleManager lifecycleManager;

    public ServiceInstanceController(LifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateInstance(@RequestBody UpdateInstanceRequest request) {

        ServiceInstance instance = lifecycleManager.updateInstance(request);

        if (instance == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("ServiceInstance mit Name " + request.getServiceName() + " nicht gefunden.");
        }

        return ResponseEntity.ok("Instanz " + request.getServiceName() + " erfolgreich aktualisiert und neu gestartet.");
    }
}