package com.example.eurekaclient.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

@Configuration
public class StartupService {

    private static final Logger log = LoggerFactory.getLogger(StartupService.class);

    @Bean
    public ApplicationRunner importAndStartServices(ServiceInstanceStore store,
                                                    LifecycleManager lifecycleManager) {
        return args -> {
            File file = new File("services.json");
            if (file.exists()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    List<ServiceInstance> instances = mapper.readValue(
                            file,
                            new TypeReference<List<ServiceInstance>>() {}
                    );

                    for (ServiceInstance instance : instances) {
                        ServiceInstance existing = store.findByServiceNameAndHostNameAndHttpPort(
                                instance.getServiceName(),
                                instance.getHostName(),
                                instance.getHttpPort()
                        );

                        if (existing != null) {
                            existing.setSecurePort(instance.getSecurePort());
                            existing.setDataCenterInfoName(instance.getDataCenterInfoName());
                            existing.setStatus(instance.getStatus());
                            existing.setIpAddr(instance.getIpAddr());
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
            } else {
                log.warn("Keine services.json gefunden – überspringe Import.");
            }

            // Jetzt alle Instanzen aus der DB holen und Lifecycle starten
            List<ServiceInstance> allInstances = store.getInstances();
            for (ServiceInstance instance : allInstances) {
                lifecycleManager.startLifecycle(instance);
                log.info("Lifecycle gestartet für: {}", instance.getServiceName());
            }
        };
    }
}
