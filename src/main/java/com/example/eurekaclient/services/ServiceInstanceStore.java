package com.example.eurekaclient.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class ServiceInstanceStore {

    private final List<ServiceInstance> instances = new CopyOnWriteArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public List<ServiceInstance> getInstances() {
        return List.copyOf(instances);
    }

    public ServiceInstance findByServiceNameAndHostNameAndHttpPort(String serviceName, String hostName, int httpPort) {
        return instances.stream()
                .filter(i -> i.getServiceName().equalsIgnoreCase(serviceName)
                        && i.getHostName().equalsIgnoreCase(hostName)
                        && i.getHttpPort() == httpPort)
                .findFirst()
                .orElse(null);
    }

    public ServiceInstance findByServiceName(String serviceName) {
        return instances.stream()
                .filter(i -> i.getServiceName().equalsIgnoreCase(serviceName))
                .findFirst()
                .orElse(null);
    }

    public synchronized void save(ServiceInstance instance) {
        if (instance.getId() == null) {
            instance.setId(idCounter.getAndIncrement());
            log.debug("[Store] Neue Instanz-ID {} für Service {}", instance.getId(), instance.getServiceName());
        }

        instances.removeIf(i -> i.getServiceName().equalsIgnoreCase(instance.getServiceName()));
        instances.add(instance);

        log.info("[Store] Instanz {} gespeichert. Gesamt: {}", instance.getServiceName(), instances.size());
    }
}
