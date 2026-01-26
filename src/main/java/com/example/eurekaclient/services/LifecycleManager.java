package com.example.eurekaclient.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class LifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(LifecycleManager.class);

    private final EurekaClientService eurekaClientService;
    private final ServiceInstanceStore serviceInstanceStore;
    private final ScheduledExecutorService scheduler;

    private final Counter registrationsCounter;
    private final Counter registrationsFailedCounter;
    private final Counter heartbeatsCounter;
    private final Counter heartbeatsFailedCounter;

    private final Map<Long, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> stopEvents = new ConcurrentHashMap<>();
    private final Map<Long, ServiceInstance> instanceMap = new ConcurrentHashMap<>();

    @Value("${lifecycle.retry.max:5}")
    private int maxRegisterRetries;

    @Value("${lifecycle.heartbeat.retry.max:50}")
    private int maxHeartbeatRetries;

    public LifecycleManager(
            EurekaClientService eurekaClientService,
            MeterRegistry meterRegistry,
            ServiceInstanceStore serviceInstanceStore
    ) {
        this.eurekaClientService = eurekaClientService;
        this.serviceInstanceStore = serviceInstanceStore;

        this.scheduler = Executors.newScheduledThreadPool(5);

        this.registrationsCounter = Counter.builder("eureka_registrations_total")
                .description("Gesamtzahl erfolgreicher Registrierungen bei Eureka")
                .register(meterRegistry);

        this.registrationsFailedCounter = Counter.builder("eureka_registrations_failed_total")
                .description("Gesamtzahl fehlgeschlagener Registrierungen bei Eureka")
                .register(meterRegistry);

        this.heartbeatsCounter = Counter.builder("eureka_heartbeats_total")
                .description("Gesamtzahl erfolgreicher Heartbeats an Eureka")
                .register(meterRegistry);

        this.heartbeatsFailedCounter = Counter.builder("eureka_heartbeats_failed_total")
                .description("Gesamtzahl fehlgeschlagener Heartbeats an Eureka")
                .register(meterRegistry);

        Gauge.builder("eureka_clients_running", this, lm -> lm.instanceMap.size())
                .description("Anzahl aktuell laufender Eureka Clients")
                .register(meterRegistry);

        Gauge.builder("eureka_clients_configured", this, lm -> lm.getConfiguredInstances())
                .description("Anzahl aktuell konfigurierter Eureka Clients")
                .register(meterRegistry);
    }

    public void startLifecycle(ServiceInstance instance) {
        retryRegister(instance, 0);
    }

    private void retryRegister(ServiceInstance instance, int attempt) {
        boolean registered = eurekaClientService.registerInstance(instance);

        if (registered) {
            log.info("[Lifecycle] Registrierung erfolgreich für {}", instance.getServiceName());
            registrationsCounter.increment();

            AtomicBoolean stopEvent = new AtomicBoolean(false);
            stopEvents.put(instance.getId(), stopEvent);
            instanceMap.put(instance.getId(), instance);

            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                if (stopEvent.get()) {
                    log.info("[Lifecycle] Stop-Signal empfangen für {}", instance.getServiceName());
                    return;
                }

                try {
                    boolean ok = eurekaClientService.sendHeartbeat(instance);
                    if (!ok) {
                        retryHeartbeat(instance, 1);
                    }
                } catch (HttpClientErrorException.NotFound nf) {
                    log.error("[Lifecycle] Heartbeat 404 für {} – erneute Registrierung", instance.getServiceName());
                    retryRegister(instance, 0);
                } catch (Exception e) {
                    log.error("[Lifecycle] Fehler beim Heartbeat für {}: {}", instance.getServiceName(), e.getMessage());
                    retryHeartbeat(instance, 1);
                }
            }, 0, 20, TimeUnit.SECONDS);

            runningTasks.put(instance.getId(), future);
            return;
        }

        registrationsFailedCounter.increment();

        if (attempt >= maxRegisterRetries) {
            log.error("[Lifecycle] Registrierung endgültig fehlgeschlagen für {} nach {} Versuchen",
                    instance.getServiceName(), attempt);
            return;
        }

        int nextAttempt = attempt + 1;
        long delay = Math.min(60, (long) Math.pow(2, attempt));

        log.warn("[Lifecycle] Registrierung fehlgeschlagen für {} – neuer Versuch in {} Sekunden (Versuch {})",
                instance.getServiceName(), delay, nextAttempt);

        scheduler.schedule(() -> retryRegister(instance, nextAttempt), delay, TimeUnit.SECONDS);
    }

    private void retryHeartbeat(ServiceInstance instance, int attempt) {
        if (attempt > maxHeartbeatRetries) {
            log.error("[Lifecycle] Heartbeat endgültig fehlgeschlagen für {} nach {} Versuchen",
                    instance.getServiceName(), attempt - 1);
            heartbeatsFailedCounter.increment();
            return;
        }

        long delay = Math.min(60, (long) Math.pow(2, attempt));

        scheduler.schedule(() -> {
            boolean ok = eurekaClientService.sendHeartbeat(instance);

            if (!ok) {
                log.warn("[Lifecycle] Heartbeat Retry {} fehlgeschlagen für {} – neuer Versuch in {} Sekunden",
                        attempt, instance.getServiceName(), delay);
                heartbeatsFailedCounter.increment();
                retryHeartbeat(instance, attempt + 1);
            } else {
                log.info("[Lifecycle] Heartbeat erfolgreich nach Retry {} für {}", attempt, instance.getServiceName());
                heartbeatsCounter.increment();
            }
        }, delay, TimeUnit.SECONDS);
    }

    public void stopLifecycle(ServiceInstance instance) {
        AtomicBoolean stopEvent = stopEvents.get(instance.getId());
        if (stopEvent != null) {
            stopEvent.set(true);
        }

        ScheduledFuture<?> future = runningTasks.remove(instance.getId());
        if (future != null) {
            future.cancel(true);
        }

        instanceMap.remove(instance.getId());
        stopEvents.remove(instance.getId());

        eurekaClientService.deregisterInstance(instance);

        log.info("[Lifecycle] Instanz gestoppt und deregistriert: {}", instance.getServiceName());
    }

    public void stopAll(List<ServiceInstance> instances) {
        instances.forEach(this::stopLifecycle);
        scheduler.shutdown();
    }

    public void stopAllRunning() {
        List<ServiceInstance> running = getRunningInstances();
        running.forEach(this::stopLifecycle);
        log.info("[Lifecycle] Alle {} laufenden Instanzen gestoppt", running.size());
    }

    public List<ServiceInstance> getRunningInstances() {
        return List.copyOf(instanceMap.values());
    }

    private int getConfiguredInstances() {
        return serviceInstanceStore.getInstances().size();
    }

    public ServiceInstance updateInstance(UpdateInstanceRequest request) {
        ServiceInstance instance = serviceInstanceStore.findByServiceName(request.getServiceName());

        if (instance == null) {
            log.warn("[Lifecycle] Update: Instanz {} nicht gefunden", request.getServiceName());
            return null;
        }

        stopLifecycle(instance);

        instance.setHostName(request.getNewHostName());
        instance.setIpAddr(request.getNewIpAddress());
        instance.setHttpPort(request.getHttpPort());
        instance.setSecurePort(request.getSecurePort());
        instance.setSslPreferred(request.isSslPreferred());

        serviceInstanceStore.save(instance);
        startLifecycle(instance);

        log.info("[Controller] Instanz {} aktualisiert: Host={}, IP={}, SecurePort={}, SSL={}",
                request.getServiceName(),
                request.getNewHostName(),
                request.getNewIpAddress(),
                request.getSecurePort(),
                request.isSslPreferred());

        return instance;
    }
}

