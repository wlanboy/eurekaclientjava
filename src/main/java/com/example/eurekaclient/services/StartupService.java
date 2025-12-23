package com.example.eurekaclient.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;

@Slf4j
@Configuration
public class StartupService {

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${store.json.path}")
    private Resource storeJsonResource;

    @Bean
    public ApplicationRunner importAndStartServices(ServiceInstanceStore store,
                                                    LifecycleManager lifecycleManager) {
        return args -> {

            String json = new String(storeJsonResource.getInputStream().readAllBytes());

            if (json.length() > 0) {
                importServices(json, store);
            } else {
                log.warn("Keine services.json gefunden – überspringe Import.");
            }

            // Lifecycle für alle Instanzen starten
            store.getInstances().forEach(instance -> {
                lifecycleManager.startLifecycle(instance);
                log.info("Lifecycle gestartet für: {}", instance.getServiceName());
            });
        };
    }

    private void importServices(String json, ServiceInstanceStore store) {
        try {
            List<ServiceInstance> instances = mapper.readValue(
                    json,
                    new TypeReference<List<ServiceInstance>>() {}
            );

            for (ServiceInstance instance : instances) {
                ServiceInstance existing = store.findByServiceNameAndHostNameAndHttpPort(
                        instance.getServiceName(),
                        instance.getHostName(),
                        instance.getHttpPort()
                );

                if (existing != null) {
                    updateExistingInstance(existing, instance);
                    store.save(existing);
                    log.info("Aktualisiert: {}", existing.getServiceName());
                } else {
                    store.save(instance);
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
