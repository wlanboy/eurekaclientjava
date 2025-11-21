package com.example.eurekaclient.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
import java.util.stream.Collectors;

@Component
public class LifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(LifecycleManager.class);

    private final EurekaClientService eurekaClientService;
    private final ServiceInstanceStore serviceInstanceStore;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    private final Counter registrationsCounter;
    private final Counter registrationsFailedCounter;
    private final Counter heartbeatsCounter;
    private final Counter heartbeatsFailedCounter;

    private final Map<Long, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> stopEvents = new ConcurrentHashMap<>();
    private final Map<Long, ServiceInstance> instanceMap = new ConcurrentHashMap<>();

    // Werte aus application.properties
    @Value("${lifecycle.retry.max:5}")
    private int maxRegisterRetries;

    @Value("${lifecycle.heartbeat.retry.max:50}")
    private int maxHeartbeatRetries;

    public LifecycleManager(EurekaClientService eurekaClientService, MeterRegistry meterRegistry, ServiceInstanceStore serviceInstanceStore) {
        this.eurekaClientService = eurekaClientService;
        this.serviceInstanceStore = serviceInstanceStore;

        // Counter für Registrierungen
        this.registrationsCounter = Counter.builder("eureka_registrations_total")
                .description("Gesamtzahl erfolgreicher Registrierungen bei Eureka")
                .register(meterRegistry);

        this.registrationsFailedCounter = Counter.builder("eureka_registrations_failed_total")
                .description("Gesamtzahl fehlgeschlagener Registrierungen bei Eureka")
                .register(meterRegistry);

        // Counter für Heartbeats
        this.heartbeatsCounter = Counter.builder("eureka_heartbeats_total")
                .description("Gesamtzahl erfolgreicher Heartbeats an Eureka")
                .register(meterRegistry);

        this.heartbeatsFailedCounter = Counter.builder("eureka_heartbeats_failed_total")
                .description("Gesamtzahl fehlgeschlagener Heartbeats an Eureka")
                .register(meterRegistry);

        // Gauge für aktuell laufende Clients
        Gauge.builder("eureka_clients_running", this, lm -> lm.getRunningInstances().size())
            .description("Anzahl aktuell laufender Eureka Clients")
            .register(meterRegistry);

        // Gauge für aktuell konfigurierte Clients
        Gauge.builder("eureka_clients_configured", this, is -> is.getConfiguredInstances())
            .description("Anzahl aktuell konfigurierten Eureka Clients")
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

            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                if (stopEvent.get()) {
                    log.info("[Lifecycle] Stop-Signal empfangen für {}", instance.getServiceName());
                    return;
                }

                try {
                    boolean ok = eurekaClientService.sendHeartbeat(instance);
                    if (!ok) {
                        CompletableFuture.delayedExecutor(4, TimeUnit.SECONDS).execute(() -> {
                            retryHeartbeat(instance, 1);
                        });
                    }
                } catch (HttpClientErrorException.NotFound nf) {
                    log.error("[Lifecycle] Heartbeat 404 für {} – erneute Registrierung", instance.getServiceName(), nf);
                    CompletableFuture.delayedExecutor(4, TimeUnit.SECONDS).execute(() -> {
                            retryRegister(instance, 0);
                        });
                } catch (Exception e) {
                    log.error("[Lifecycle] Fehler beim Heartbeat für {}: {}", instance.getServiceName(), e.getMessage(), e);
                    retryHeartbeat(instance, 1);
                }
            }, 0, 20, TimeUnit.SECONDS);

            runningTasks.put(instance.getId(), future);
            instanceMap.put(instance.getId(), instance);

        } else {
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
                log.info("[Lifecycle] Heartbeat erfolgreich nach Retry {} für {}",
                        attempt, instance.getServiceName());
                heartbeatsCounter.increment();
            }
        }, delay, TimeUnit.SECONDS);
    }

    public void stopLifecycle(ServiceInstance instance) {
        AtomicBoolean stopEvent = stopEvents.get(instance.getId());
        if (stopEvent != null) {
            stopEvent.set(true);
        }
        ScheduledFuture<?> future = runningTasks.get(instance.getId());
        if (future != null) {
            future.cancel(true);
        }
        eurekaClientService.deregisterInstance(instance);
        log.info("[Lifecycle] Instanz gestoppt und deregistriert: {}", instance.getServiceName());
    }

    public void stopAll(List<ServiceInstance> instances) {
        for (ServiceInstance instance : instances) {
            stopLifecycle(instance);
        }
        scheduler.shutdown();
    }

    public List<ServiceInstance> getRunningInstances() {
        return instanceMap.values().stream().collect(Collectors.toList());
    }

    private int getConfiguredInstances() {
        return serviceInstanceStore.getInstances().size();
    }
}
