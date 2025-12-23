package com.example.eurekaclient;

import com.example.eurekaclient.services.LifecycleManager;
import com.example.eurekaclient.services.ServiceInstanceStore;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class Application {

    private final ServiceInstanceStore store;
    private final LifecycleManager lifecycleManager;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        log.info(">>> Eureka Client Application gestartet.");
    }

    @PreDestroy
    public void onShutdown() {
        log.info(">>> Anwendung wird heruntergefahren – stoppe alle Eureka Clients...");

        lifecycleManager.stopAll(store.getInstances());

        log.info(">>> Alle Clients gestoppt und deregistriert.");
    }

}
