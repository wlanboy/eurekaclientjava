package com.example.eurekaclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.eurekaclient.services.LifecycleManager;
import com.example.eurekaclient.services.ServiceInstance;
import com.example.eurekaclient.services.ServiceInstanceStore;

import jakarta.annotation.PreDestroy;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final ServiceInstanceStore store;
    private final LifecycleManager lifecycleManager;

    public Application(ServiceInstanceStore store, LifecycleManager lifecycleManager) {
        this.store = store;
        this.lifecycleManager = lifecycleManager;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        log.info(">>> Eureka Client Application gestartet.");
    }

    @PreDestroy
    public void onShutdown() {
        log.info(">>> Anwendung wird heruntergefahren – stoppe alle Eureka Clients...");
        List<ServiceInstance> allInstances = store.getInstances();
        lifecycleManager.stopAll(allInstances);
        log.info(">>> Alle Clients gestoppt und deregistriert.");
    }
}
