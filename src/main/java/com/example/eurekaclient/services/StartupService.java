package com.example.eurekaclient.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class StartupService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ServiceInstanceStore store;

    @Value("${store.json.path}")
    private Resource storeJsonResource;

    public StartupService(ServiceInstanceStore store) {
        this.store = store;
    }

    @Bean
    public ApplicationRunner importAndStartServices(LifecycleManager lifecycleManager) {
        return args -> {
            loadAndImportServices();
            startAllLifecycles(lifecycleManager);
        };
    }

    public void loadAndImportServices() throws IOException {
        String json = new String(storeJsonResource.getInputStream().readAllBytes());

        if (json.length() > 0) {
            importServices(json);
        } else {
            log.warn("Keine services.json gefunden – überspringe Import.");
        }
    }

    public void startAllLifecycles(LifecycleManager lifecycleManager) {
        store.getInstances().forEach(instance -> {
            lifecycleManager.startLifecycle(instance);
            log.info("Lifecycle gestartet für: {}", instance.getServiceName());
        });
    }

    private void importServices(String json) {
        try {
            List<ServiceInstance> instances = mapper.readValue(
                    json,
                    new TypeReference<List<ServiceInstance>>() {}
            );

            for (ServiceInstance instance : instances) {
                ServiceInstance existing = this.store.findByServiceNameAndHostNameAndHttpPort(
                        instance.getServiceName(),
                        instance.getHostName(),
                        instance.getHttpPort()
                );

                if (existing != null) {
                    updateExistingInstance(existing, instance);
                    this.store.save(existing);
                    log.info("Aktualisiert: {}", existing.getServiceName());
                } else {
                    this.store.save(instance);
                    log.info("Neu importiert: {}", instance.getServiceName());
                }
            }

        } catch (Exception e) {
            log.error("Fehler beim Import von services.json: {}", e.getMessage(), e);
        }
    }

    private void updateExistingInstance(ServiceInstance existing, ServiceInstance imported) {
        existing.setSecurePort(imported.getSecurePort());
        existing.setDataCenterInfoName(imported.getDataCenterInfoName());
        existing.setStatus(imported.getStatus());
        existing.setIpAddr(imported.getIpAddr());
    }
}
